$path1 = "C:\Users\Diatom\Desktop\YouTube_Reference_Tool"
$path2 = "C:\Users\Diatom\Desktop\YouTube Transcripts"

# Open both directories as separate VS Code instances
Start-Process "code" -ArgumentList "`"$path1`""
Start-Sleep -Milliseconds 800
Start-Process "code" -ArgumentList "`"$path2`""

# Wait for VS Code windows to fully render
Start-Sleep -Seconds 3

# Win32 helpers to move windows
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
    [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr h, int x, int y, int w, int hh, bool r);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
}
"@

# Display 2 (\\.\DISPLAY1) is the 4K panel starting at X=1920, 3840x2160
# Split it side-by-side: each VS Code gets half the 4K width
$d2x    = 1920
$d2y    = 0
$halfW  = 1920   # half of 3840
$fullH  = 2160

$procs = Get-Process -Name "Code" -ErrorAction SilentlyContinue |
         Where-Object { $_.MainWindowHandle -ne [IntPtr]::Zero } |
         Sort-Object StartTime -Descending |
         Select-Object -First 2

if ($procs.Count -eq 2) {
    # Newest window (YouTube Transcripts) → right half
    [Win32]::ShowWindow($procs[0].MainWindowHandle, 9)  | Out-Null
    [Win32]::MoveWindow($procs[0].MainWindowHandle, ($d2x + $halfW), $d2y, $halfW, $fullH, $true) | Out-Null

    # Older window (YouTube_Reference_Tool) → left half
    [Win32]::ShowWindow($procs[1].MainWindowHandle, 9)  | Out-Null
    [Win32]::MoveWindow($procs[1].MainWindowHandle, $d2x, $d2y, $halfW, $fullH, $true) | Out-Null

    Write-Host "Positioned both VS Code windows on Display 2"
} else {
    Write-Host "Found $($procs.Count) VS Code window(s) — expected 2"
}
