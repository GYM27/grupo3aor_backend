const auth = await fetch("http://localhost:8080/api/auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email: "admin@gmail.com", password: "admin" })
}).then(r => r.json());

const token = auth.token;

const sim = await fetch("http://localhost:8080/api/simulations/start", {
  method: "POST",
  headers: { 
    "Content-Type": "application/json",
    "Authorization": "Bearer " + token
  },
  body: JSON.stringify({ scenarioId: 1 })
}).then(r => r.json());

console.log("Started simulation:", sim.id, "Status:", sim.status);

const stop1 = await fetch(`http://localhost:8080/api/simulations/${sim.id}/stop?cutOffSeconds=0.02`, {
  method: "POST",
  headers: { "Authorization": "Bearer " + token }
});

console.log("Stop 1 status:", stop1.status);
console.log("Stop 1 response:", await stop1.text());

const stop2 = await fetch(`http://localhost:8080/api/simulations/${sim.id}/stop?cutOffSeconds=0.02`, {
  method: "POST",
  headers: { "Authorization": "Bearer " + token }
});

console.log("Stop 2 status:", stop2.status);
console.log("Stop 2 response:", await stop2.text());
