// Debug script
const https = require("http");

function post(url, body, token) {
    return new Promise((resolve) => {
        const urlObj = new URL(url);
        const data = JSON.stringify(body);
        const options = {
            hostname: urlObj.hostname, port: urlObj.port,
            path: urlObj.pathname + urlObj.search, method: "POST",
            headers: { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(data) },
        };
        if (token) options.headers["Authorization"] = "Bearer " + token;
        const req = https.request(options, (res) => {
            let b = ""; res.on("data", (c) => (b += c));
            res.on("end", () => { try { resolve(JSON.parse(b)); } catch { resolve({ raw: b }); } });
        });
        req.write(data); req.end();
    });
}

function get(url, token) {
    return new Promise((resolve) => {
        const urlObj = new URL(url);
        const options = {
            hostname: urlObj.hostname, port: urlObj.port,
            path: urlObj.pathname + urlObj.search, method: "GET", headers: {},
        };
        if (token) options.headers["Authorization"] = "Bearer " + token;
        const req = https.request(options, (res) => {
            let b = ""; res.on("data", (c) => (b += c));
            res.on("end", () => { try { resolve(JSON.parse(b)); } catch { resolve({ raw: b }); } });
        });
        req.end();
    });
}

(async () => {
    const loginR = await post("http://localhost:4000/api/auth/login", { email: "admin@gmail.com", password: "admin123" });
    const token = loginR.data && loginR.data.token;
    console.log("Token OK:", !!token);

    const createR = await post("http://localhost:4000/api/projects", {
        title: "Thanh Mau Debug",
        description: "Test",
        format: "MANGA"
    }, token);
    console.log("Create project result:", JSON.stringify(createR).slice(0, 500));
})();
