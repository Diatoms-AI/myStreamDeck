CreateObject("WScript.Shell").Run "python """ & CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName) & "\tray.pyw""", 0, False
