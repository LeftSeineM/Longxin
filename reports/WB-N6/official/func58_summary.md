# WB-N6 official functional result

- Result: **58 / 58 PASS**
- Failed points: none
- Test image: NSCSCC 2026 `nscscc_func/obj/main.bin`
- Core: `generated/WB-N6`
- Simulator/tool: Vivado 2025.2 XSim
- Bounded run setting: `100ms`
- Last completed point: 58
- Completion simulation time: 5,699,205 ns
- Queue contract assertion failures: none
- Source log:
  `D:\NSCSCC2026\PulseLA\build\wb_n6_fetchqueue_func58\vivado_official_func.log`

Command:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File D:\NSCSCC2026\PulseLA\scripts\run_official_func.ps1 `
  -RunTime 100ms -ExpectedLastPoint 58 `
  -ExternalCoreRoot D:\NSCSCC2026\WeBattle-Core\generated\WB-N6 `
  -OutputName wb_n6_fetchqueue_func58
```
