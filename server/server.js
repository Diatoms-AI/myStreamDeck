const http = require('http');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const PORT     = 8765;
const ACTIONS  = path.join(__dirname, 'actions');
const MACROS   = path.join(__dirname, 'macros');
const PYTHON   = 'python';

if (!fs.existsSync(MACROS)) fs.mkdirSync(MACROS);

// ── Recording state ──────────────────────────────────────────────────────────
let recorderProc    = null;
let recordingButton = null;

// ── Helpers ──────────────────────────────────────────────────────────────────
function runPowerShell(script) {
    const ps = spawn('powershell', ['-ExecutionPolicy', 'Bypass', '-File', script]);
    ps.stdout.on('data', d => console.log('[ps]', d.toString().trim()));
    ps.stderr.on('data', d => console.error('[ps]', d.toString().trim()));
    ps.on('close', code => console.log(`[ps] exit ${code}`));
}

function runPython(args) {
    const py = spawn(PYTHON, args);
    py.stdout.on('data', d => console.log('[py]', d.toString().trim()));
    py.stderr.on('data', d => console.error('[py]', d.toString().trim()));
    py.on('close', code => console.log(`[py] exit ${code}`));
}

function playButton(id) {
    const macroFile = path.join(MACROS, `button${id}.json`);
    if (fs.existsSync(macroFile)) {
        console.log(`[button ${id}] playing macro`);
        runPython([path.join(__dirname, 'player.py'), macroFile]);
        return;
    }
    const ps1 = path.join(ACTIONS, `button${id}.ps1`);
    if (fs.existsSync(ps1)) {
        console.log(`[button ${id}] running script`);
        runPowerShell(ps1);
        return;
    }
    console.log(`[button ${id}] no action configured`);
}

function macroEventCount(buttonId) {
    const f = path.join(MACROS, `button${buttonId}.json`);
    if (!fs.existsSync(f)) return 0;
    try { return JSON.parse(fs.readFileSync(f)).length; } catch { return 0; }
}

function json(res, code, obj) {
    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.writeHead(code);
    res.end(JSON.stringify(obj));
}

// ── Server ───────────────────────────────────────────────────────────────────
const server = http.createServer((req, res) => {
    const { method, url } = req;
    console.log(`[${new Date().toLocaleTimeString()}] ${method} ${url}`);

    // Health
    if (url === '/health') {
        return json(res, 200, { ok: true, server: 'myStreamDeck' });
    }

    // Button press — dynamic (handles all 15)
    const btnMatch = url.match(/^\/button\/(\d+)$/);
    if (btnMatch && (method === 'POST' || method === 'GET')) {
        const id = parseInt(btnMatch[1]);
        json(res, 200, { ok: true, button: id });
        playButton(id);
        return;
    }

    // Start recording
    if (url.startsWith('/record/start') && method === 'POST') {
        const id = parseInt(new URL(url, 'http://x').searchParams.get('id') || '0');
        if (recorderProc) { recorderProc.kill(); recorderProc = null; }
        recordingButton = id;
        const macroFile = path.join(MACROS, `button${id}.json`);
        // Clear previous macro so event count starts from 0
        if (fs.existsSync(macroFile)) fs.unlinkSync(macroFile);
        recorderProc = spawn(PYTHON, [path.join(__dirname, 'recorder.py'), macroFile]);
        recorderProc.on('close', () => { recorderProc = null; });
        console.log(`[record] started for button ${id}`);
        return json(res, 200, { ok: true, recording: true, buttonId: id });
    }

    // Stop recording
    if (url === '/record/stop' && method === 'POST') {
        if (recorderProc) {
            recorderProc.kill();
            recorderProc = null;
        }
        const savedId = recordingButton;
        const count   = macroEventCount(savedId);
        recordingButton = null;
        console.log(`[record] stopped — ${count} events saved for button ${savedId}`);
        return json(res, 200, { ok: true, recording: false, buttonId: savedId, eventCount: count });
    }

    // Recording status (polled by Android every second)
    if (url === '/record/status') {
        return json(res, 200, {
            recording:  recorderProc !== null,
            buttonId:   recordingButton,
            eventCount: recordingButton !== null ? macroEventCount(recordingButton) : 0,
        });
    }

    json(res, 404, { error: `No handler for ${method} ${url}` });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log('');
    console.log('  myStreamDeck server running');
    console.log(`  Android URL : http://localhost:${PORT}/button/<id>`);
    console.log(`  Health      : http://localhost:${PORT}/health`);
    console.log(`  Record      : POST /record/start?id=<n>  |  POST /record/stop`);
    console.log('');
});
