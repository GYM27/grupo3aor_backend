import fs from 'fs';

// Configurações base
const BASE_URL = 'http://localhost:8080/api';
const EMAIL = 'admin@gmail.com';
const PASSWORD = 'Pass1234#';

async function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// Helper para extrair cookies
function extractCookies(response, currentCookies) {
    const raw = response.headers.getRaw ? response.headers.getRaw('set-cookie') : response.headers.get('set-cookie');
    if (!raw) return currentCookies;
    
    // Suporte Node 18+ (get returns comma separated string, getRaw returns array)
    const cookiesArray = Array.isArray(raw) ? raw : raw.split(/,(?=\s*[a-zA-Z0-9_-]+\=)/);
    
    cookiesArray.forEach(cookieStr => {
        const parts = cookieStr.split(';')[0].split('=');
        if (parts.length === 2) {
            currentCookies[parts[0].trim()] = parts[1].trim();
        }
    });
    return currentCookies;
}

function getCookieString(cookies) {
    return Object.entries(cookies).map(([k, v]) => `${k}=${v}`).join('; ');
}

async function runDemo() {
    console.log("=== INICIANDO SIMULADOR DE STREAM ===");
    let cookies = {};

    // 1. Obter CSRF Token inicial (chamando um endpoint público ou fazendo login)
    console.log("1. Autenticando com " + EMAIL + "...");
    const loginRes = await fetch(`${BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: EMAIL, password: PASSWORD })
    });
    
    if (!loginRes.ok) {
        console.error("Erro no login!", await loginRes.text());
        return;
    }
    cookies = extractCookies(loginRes, cookies);
    console.log("-> Login efetuado com sucesso! Sessão obtida.\n");

    // Fazer um pedido GET para forçar a receção do XSRF-TOKEN (se não veio no login)
    const initRes = await fetch(`${BASE_URL}/clinical-scenarios`, {
        headers: { 'Cookie': getCookieString(cookies) }
    });
    cookies = extractCookies(initRes, cookies);

    const headers = { 
        'Content-Type': 'application/json',
        'Cookie': getCookieString(cookies),
        'X-XSRF-TOKEN': cookies['XSRF-TOKEN'] || ''
    };
    console.log("2. Verificando cenários na BD...");
    let scenariosRes = await fetch(`${BASE_URL}/clinical-scenarios`, { headers });
    let scenarios = await scenariosRes.json();
    
    // Se não houver cenários, vamos fazer upload do ficheiro cenario_com_dados.json automaticamente!
    if (scenarios.length === 0) {
        console.log("-> Nenhum cenário encontrado! A tentar criar o cenário a partir de cenario_com_dados.json...");
        try {
            const scenarioData = fs.readFileSync('cenario_com_dados.json', 'utf8');
            const uploadRes = await fetch(`${BASE_URL}/clinical-scenarios`, {
                method: 'POST',
                headers: headers,
                body: scenarioData
            });
            if (uploadRes.ok) {
                console.log("-> Cenário criado com sucesso na BD!");
                scenariosRes = await fetch(`${BASE_URL}/clinical-scenarios`, { headers });
                scenarios = await scenariosRes.json();
            } else {
                console.error("-> Falha ao criar cenário:", await uploadRes.text());
                return;
            }
        } catch (e) {
            console.error("-> Erro: Não foi possível ler/criar o ficheiro cenario_com_dados.json. Por favor cria um cenário na Dashboard primeiro.");
            return;
        }
    }

    // 2. Procurar ou criar um cenário dedicado para o Stream (para que o motor não o termine)
    console.log("\n2. Verificando cenários na BD...");
    let streamScenario = scenarios.find(s => s.name === "Stream Externo");
    let scenarioId;
    
    if (streamScenario) {
        scenarioId = streamScenario.id;
        console.log(`-> Cenário 'Stream Externo' encontrado: ID ${scenarioId}\n`);
    } else {
        console.log("-> A criar cenário 'Stream Externo' para evitar término automático...");
        // Usa upload FormData para contornar endpoints de JSON simples se não existirem
        const formData = new FormData();
        const blob = new Blob(['[]'], { type: 'application/json' });
        formData.append('file', blob, 'stream_externo.json');
        
        const createScenRes = await fetch(`${BASE_URL}/scenarios/upload`, {
            method: 'POST',
            headers: {
                'Cookie': headers['Cookie'],
                'X-XSRF-TOKEN': headers['X-XSRF-TOKEN']
            },
            body: formData
        });
        
        if (createScenRes.ok) {
            const newScenario = await createScenRes.json();
            scenarioId = newScenario.id;
            console.log(`-> Cenário 'Stream Externo' criado: ID ${scenarioId}\n`);
        } else {
            console.log("-> Fallback para o primeiro cenário...");
            scenarioId = scenarios[0].id;
        }
    }

    // 3. Obter ou Iniciar Simulação Live
    let simulationId = null;
    
    console.log("3. A verificar se existe alguma simulação ativa...");
    const historyRes = await fetch(`${BASE_URL}/simulations`, { headers });
    const historyData = await historyRes.json();
    
    // Procura por uma simulação que esteja EM_CURSO
    const runningSim = historyData.find(s => s.status === 'EM_CURSO' || s.status === 'INICIADA');
    
    if (runningSim) {
        simulationId = runningSim.id;
        console.log(`-> Simulação ativa encontrada! O script vai acoplar-se ao ID: ${simulationId}\n`);
    } else {
        console.log("-> Nenhuma simulação ativa. A criar uma NOVA Simulação LIVE no Backend...");
        const simRes = await fetch(`${BASE_URL}/simulations/start`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ scenarioId: scenarioId })
        });
        
        if (!simRes.ok) {
            console.error("Erro ao criar simulação", await simRes.text());
            return;
        }
        const simulation = await simRes.json();
        simulationId = simulation.id;
        console.log(`-> Simulação LIVE criada! ID: ${simulationId}\n`);
    }
    
    console.log("==================================================");
    console.log("🚨 ATENÇÃO: Abre o Dashboard no teu browser agora!");
    console.log("A preparar a leitura do ficheiro AsthmaAttackResults.csv...");
    console.log("==================================================\n");

    let csvData;
    try {
        csvData = fs.readFileSync('AsthmaAttackResults.csv', 'utf8');
    } catch (e) {
        console.error("Erro ao ler o ficheiro AsthmaAttackResults.csv. Confirma que o ficheiro existe na diretoria.");
        return;
    }

    const lines = csvData.split('\n');
    if (lines.length < 2) {
        console.error("O ficheiro CSV parece estar vazio.");
        return;
    }

    console.log("Ficheiro carregado com sucesso. A iniciar stream em tempo real...\n");

    // Índices com base no cabeçalho do BioGears:
    // 0: Time, 2: TidalVolume, 3: OxygenSaturation, 5: HeartRate, 9: RespirationRate, 11: ArterialBloodPH
    
    // Como o ficheiro tem leituras a cada 0.02s, vamos saltar de 50 em 50 linhas para simular 1 segundo real.
    // Vamos começar na linha 1 (ignorar cabeçalho).
    let i = 1;
    let seconds = 0;
    const scriptStartTime = Date.now();

    while (i < lines.length) {
        const line = lines[i].trim();
        if (!line) {
            i += 50;
            continue;
        }

        const cols = line.split(',');
        if (cols.length < 12) {
            i += 50;
            continue;
        }

        seconds++;
        
        let time = parseFloat(cols[0]);
        let tidalVol = parseFloat(cols[2]);
        
        // Arredondar valores de sinais vitais principais para números inteiros (evitar decimais gigantes)
        let spo2 = Math.round(parseFloat(cols[3]) * 100); 
        let hr = Math.round(parseFloat(cols[5]));
        let rr = Math.round(parseFloat(cols[9]));
        let ph = parseFloat(cols[11]);
        
        // O ficheiro CSV não tem Diastolic, tem MAP (index 7) e Systolic (index 8).
        // A fórmula para MAP é: MAP = (Systolic + 2 * Diastolic) / 3
        // Por isso: Diastolic = (3 * MAP - Systolic) / 2
        let mapBp = parseFloat(cols[7]);
        let sysBp = Math.round(parseFloat(cols[8]));
        let diaBp = Math.round((3 * mapBp - sysBp) / 2);

        const metrics = [
            { simulationId, handle: "HEART_RATE", unit: "bpm", value: hr, timestamp: new Date().toISOString() },
            { simulationId, handle: "RR", unit: "bpm", value: rr, timestamp: new Date().toISOString() },
            { simulationId, handle: "SPO2", unit: "%", value: spo2, timestamp: new Date().toISOString() },
            { simulationId, handle: "ARTERIALPRESSURE_SYSTOLIC", unit: "mmHg", value: sysBp, timestamp: new Date().toISOString() },
            { simulationId, handle: "ARTERIALPRESSURE_DIASTOLIC", unit: "mmHg", value: diaBp, timestamp: new Date().toISOString() },
            { simulationId, handle: "TidalVolume", unit: "mL", value: tidalVol, timestamp: new Date().toISOString() },
            { simulationId, handle: "ArterialBloodPH", unit: "unit", value: ph, timestamp: new Date().toISOString() }
        ];

        let isSimulationClosed = false;

        const res = await fetch(`${BASE_URL}/readings/batch`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(metrics)
        }).catch(e => { return { ok: false, status: 500 }; });

        if (!res.ok && (res.status === 400 || res.status === 500)) {
            isSimulationClosed = true;
        }

        if (isSimulationClosed) {
            console.log("\n⚠️ Simulação terminada no servidor pelo Dashboard. A criar nova sessão Limpa...");
            const simRes = await fetch(`${BASE_URL}/simulations/start`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ scenarioId: scenarioId })
            });
            
            if (simRes.ok) {
                const simulation = await simRes.json();
                simulationId = simulation.id;
                console.log(`-> NOVA Simulação LIVE criada! ID: ${simulationId}\n`);
                // Não fazemos continue, apenas atualizamos o ID. A métrica atual perde-se, mas o loop avança normalmente.
            } else {
                console.error("Erro crítico ao recriar simulação.");
                break;
            }
        } else {
            process.stdout.write(`⏱️ Tempo da Simulação: ${time.toFixed(1)}s | HR: ${hr.toFixed(1)} bpm | SPO2: ${spo2.toFixed(1)}% | RR: ${rr.toFixed(1)} \r`);
        }
        
        // Pular 50 linhas (0.02s * 50 = 1s de simulação)
        i += 50;
        
        // Drift management
        const expectedTime = seconds * 1000;
        const actualTime = Date.now() - scriptStartTime;
        const drift = actualTime - expectedTime;
        await sleep(Math.max(0, 1000 - drift));
    }
    
    console.log("\nFim do ficheiro alcançado. O Stream terminou.");
}

runDemo();
