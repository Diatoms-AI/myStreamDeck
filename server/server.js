const http = require('http');
const { spawn } = require('child_process');
const path = require('path');

const PORT = 8765;
const ACTIONS = path.join(__dirname, 'actions');

// Map every button to its PowerShell action script
const routes = {
    'POST /button/1':  'button1.ps1',
    'GET  /button/1':  'button1.ps1',   // also accept GET for easy browser testing
    'GET  /health':    null,
};

function runScript(script) {
    return new Promise((resolve) => {
        const ps = spawn('powershell', [
            '-ExecutionPolicy', 'Bypass',
            '-File', path.join(ACTIONS, script)
        ]);
        ps.stdout.on('data', d => console.log('[ps]', d.toString().trim()));
        ps.stderr.on('data', d => console.error('[ps]', d.toString().trim()));
        ps.on('close', code => resolve({ ok: code === 0, exitCode: code }));
        ps.on('error', err => resolve({ ok: false, error: err.message }));
    });
}

const server = http.createServer((req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Access-Control-Allow-Origin', '*');

    const key = `${req.method} ${req.url}`;
    console.log(`[${new Date().toLocaleTimeString()}] ${key}`);

    if (req.url === '/health') {
        res.writeHead(200);
        res.end(JSON.stringify({ ok: true, server: 'myStreamDeck' }));
        return;
    }

    const script = routes[key];
    if (!script) {
        res.writeHead(404);
        res.end(JSON.stringify({ error: `No action mapped to: ${key}` }));
        return;
    }

    // Respond immediately — fire the script in background
    res.writeHead(200);
    res.end(JSON.stringify({ ok: true, action: key }));
    runScript(script).then(r => console.log(`[done]`, r));
});

server.listen(PORT, '0.0.0.0', () => {
    console.log('');
    console.log('  myStreamDeck server running');
    console.log(`  Android URL: http://10.158.12.5:${PORT}/button/<id>`);
    console.log(`  Health:      http://10.158.12.5:${PORT}/health`);
    console.log('');
});
