Import-Module VirtualDesktop -WarningAction SilentlyContinue

# Find YouTube desktop by name
$targetIndex = -1
$count = Get-DesktopCount
for ($i = 0; $i -lt $count; $i++) {
    $d = Get-Desktop $i
    if ((Get-DesktopName $d) -eq "YouTube") {
        $targetIndex = $i
        break
    }
}
if ($targetIndex -lt 0) { Write-Host "No desktop named 'YouTube' found"; exit 1 }

# Switch to YouTube desktop first — exact delta so VS Code opens there
$currentIndex = Get-DesktopIndex (Get-CurrentDesktop)
$delta = $targetIndex - $currentIndex
$python   = "$env:LOCALAPPDATA\Microsoft\WindowsApps\python.exe"
$switcher = "$PSScriptRoot\..\switch_desktop.py"
& $python $switcher $delta
Start-Sleep -Milliseconds 800

$path1 = "C:\Users\Diatom\Desktop\YouTube_Reference_Tool"
$path2 = "C:\Users\Diatom\Desktop\YouTube Transcripts"

Start-Process "code" -ArgumentList "`"$path1`""
Start-Sleep -Milliseconds 800
Start-Process "code" -ArgumentList "`"$path2`""
Start-Sleep -Seconds 3

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
    [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr h, int x, int y, int w, int hh, bool r);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
}
"@

$d2x   = 1920
$d2y   = 0
$halfW = 1920
$fullH = 2160

$procs = Get-Process -Name "Code" -ErrorAction SilentlyContinue |
         Where-Object { $_.MainWindowHandle -ne [IntPtr]::Zero } |
         Sort-Object StartTime -Descending |
         Select-Object -First 2

if ($procs.Count -eq 2) {
    # Position: newest (Transcripts) → right half of 4K display
    [Win32]::ShowWindow($procs[0].MainWindowHandle, 9)  | Out-Null
    [Win32]::MoveWindow($procs[0].MainWindowHandle, ($d2x + $halfW), $d2y, $halfW, $fullH, $true) | Out-Null

    # Older (Reference Tool) → left half
    [Win32]::ShowWindow($procs[1].MainWindowHandle, 9)  | Out-Null
    [Win32]::MoveWindow($procs[1].MainWindowHandle, $d2x, $d2y, $halfW, $fullH, $true) | Out-Null

    Write-Host "Positioned both VS Code windows on YouTube desktop"
} else {
    Write-Host "Found $($procs.Count) VS Code window(s) - expected 2"
}
