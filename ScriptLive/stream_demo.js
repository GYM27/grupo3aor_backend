import fs from "fs";
import readline from "readline";

// Configurações base
const BASE_URL = "http://localhost:8080/api";
const EMAIL = "admin@gmail.com";
const PASSWORD = "Pass1234#";

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// Helper para extrair cookies
function extractCookies(response, currentCookies) {
  const raw = response.headers.getRaw
    ? response.headers.getRaw("set-cookie")
    : response.headers.get("set-cookie");
  if (!raw) return currentCookies;

  // Suporte Node 18+ (get returns comma separated string, getRaw returns array)
  const cookiesArray = Array.isArray(raw)
    ? raw
    : raw.split(/,(?=\s*[a-zA-Z0-9_-]+\=)/);

  cookiesArray.forEach((cookieStr) => {
    const parts = cookieStr.split(";")[0].split("=");
    if (parts.length === 2) {
      currentCookies[parts[0].trim()] = parts[1].trim();
    }
  });
  return currentCookies;
}

function getCookieString(cookies) {
  return Object.entries(cookies)
    .map(([k, v]) => `${k}=${v}`)
    .join("; ");
}

async function runDemo() {
  console.log("=== INICIANDO SIMULADOR DE STREAM (MODO IOT) ===");

  let simulationId = process.argv[2];
  if (!simulationId) {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });
    
    simulationId = await new Promise((resolve) => {
      rl.question("👉 Por favor, cola aqui o ID da Simulação (visível no Dashboard): ", (answer) => {
        rl.close();
        resolve(answer.trim());
      });
    });

    if (!simulationId) {
      console.error("ERRO: O ID não pode ser vazio. O script vai terminar.");
      process.exit(1);
    }
  }

  console.log(`-> O script vai acoplar-se à Simulação ID: ${simulationId}\n`);

  let cookies = {};

  // 1. Obter CSRF Token inicial (Atuando como Service Account para ter acesso à API protegida)
  console.log("1. Autenticando com " + EMAIL + " (Service Account)...");
  const loginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: EMAIL, password: PASSWORD }),
  });

  if (!loginRes.ok) {
    console.error("Erro no login!", await loginRes.text());
    return;
  }
  cookies = extractCookies(loginRes, cookies);
  console.log("-> Login efetuado com sucesso! Sessão obtida.\n");

  // Fazer um pedido GET genérico para forçar a receção do XSRF-TOKEN
  const initRes = await fetch(`${BASE_URL}/clinical-scenarios`, {
    headers: { Cookie: getCookieString(cookies) },
  });
  cookies = extractCookies(initRes, cookies);

  const headers = {
    "Content-Type": "application/json",
    Cookie: getCookieString(cookies),
    "X-XSRF-TOKEN": cookies["XSRF-TOKEN"] || "",
  };

  console.log("==================================================");
  console.log("🚨 ATENÇÃO: O Stream está a empurrar dados para a Simulação " + simulationId);
  console.log("A preparar a leitura do ficheiro AsthmaAttackResults.csv...");
  console.log("==================================================\n");

  let csvData;
  try {
    csvData = fs.readFileSync("AsthmaAttackResults.csv", "utf8");
  } catch (e) {
    console.error(
      "Erro ao ler o ficheiro AsthmaAttackResults.csv. Confirma que o ficheiro existe na diretoria.",
    );
    return;
  }

  const lines = csvData.split("\n");
  if (lines.length < 2) {
    console.error("O ficheiro CSV parece estar vazio.");
    return;
  }

  console.log(
    "Ficheiro carregado com sucesso. A iniciar stream em tempo real...\n",
  );

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

    const cols = line.split(",");
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
      {
        simulationId,
        handle: "HEART_RATE",
        unit: "bpm",
        value: hr,
        timestamp: new Date().toISOString(),
      },
      {
        simulationId,
        handle: "RR",
        unit: "bpm",
        value: rr,
        timestamp: new Date().toISOString(),
      },
      {
        simulationId,
        handle: "SPO2",
        unit: "%",
        value: spo2,
        timestamp: new Date().toISOString(),
      },
      {
        simulationId,
        handle: "ARTERIALPRESSURE_SYSTOLIC",
        unit: "mmHg",
        value: sysBp,
        timestamp: new Date().toISOString(),
      },
      {
        simulationId,
        handle: "ARTERIALPRESSURE_DIASTOLIC",
        unit: "mmHg",
        value: diaBp,
        timestamp: new Date().toISOString(),
      },
      {
        simulationId,
        handle: "TidalVolume",
        unit: "mL",
        value: tidalVol,
        timestamp: new Date().toISOString(),
      },
      {
        simulationId,
        handle: "ArterialBloodPH",
        unit: "unit",
        value: ph,
        timestamp: new Date().toISOString(),
      },
    ];

    let isSimulationClosed = false;

    const res = await fetch(`${BASE_URL}/readings/batch`, {
      method: "POST",
      headers: headers,
      body: JSON.stringify(metrics),
    }).catch((e) => {
      return { ok: false, status: 500 };
    });

    if (!res.ok && (res.status === 400 || res.status === 500)) {
      isSimulationClosed = true;
    }

    if (isSimulationClosed) {
      console.log(
        "\n⚠️ Simulação terminada (400 ou 500 recebido). O stream vai terminar."
      );
      break;
    } else {
      process.stdout.write(
        `⏱️ Tempo da Simulação: ${time.toFixed(1)}s | HR: ${hr.toFixed(1)} bpm | SPO2: ${spo2.toFixed(1)}% | RR: ${rr.toFixed(1)} \r`,
      );
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
