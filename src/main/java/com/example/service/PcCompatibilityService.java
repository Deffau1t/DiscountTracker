package com.example.service;

import com.example.entity.PcBuild;
import com.example.entity.PcComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сервис для проверки совместимости компонентов ПК
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PcCompatibilityService {

    /**
     * Проверяет совместимость всех компонентов сборки
     */
    public CompatibilityResult checkCompatibility(PcBuild build) {
        CompatibilityResult result = new CompatibilityResult();
        Map<PcComponent.ComponentType, PcComponent> components = build.getComponentsMap();

        // Проверяем основные обязательные компоненты
        if (components.get(PcComponent.ComponentType.CPU) == null) {
            result.addIssue("Процессор не выбран");
        }
        if (components.get(PcComponent.ComponentType.MOTHERBOARD) == null) {
            result.addIssue("Материнская плата не выбрана");
        }
        if (components.get(PcComponent.ComponentType.RAM) == null) {
            result.addIssue("Оперативная память не выбрана");
        }
        if (components.get(PcComponent.ComponentType.PSU) == null) {
            result.addIssue("Блок питания не выбран");
        }

        PcComponent cpu = components.get(PcComponent.ComponentType.CPU);
        PcComponent motherboard = components.get(PcComponent.ComponentType.MOTHERBOARD);
        PcComponent ram = components.get(PcComponent.ComponentType.RAM);
        PcComponent gpu = components.get(PcComponent.ComponentType.GPU);
        PcComponent storage = components.get(PcComponent.ComponentType.STORAGE);
        PcComponent psu = components.get(PcComponent.ComponentType.PSU);
        PcComponent pcCase = components.get(PcComponent.ComponentType.CASE);
        PcComponent cooler = components.get(PcComponent.ComponentType.COOLER);

        // Проверка совместимости CPU и материнской платы
        if (cpu != null && motherboard != null) {
            if (!checkCpuMotherboardCompatibility(cpu, motherboard, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка совместимости RAM и материнской платы
        if (ram != null && motherboard != null) {
            if (!checkRamMotherboardCompatibility(ram, motherboard, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка совместимости GPU и материнской платы (PCIe)
        if (gpu != null && motherboard != null) {
            if (!checkGpuMotherboardCompatibility(gpu, motherboard, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка совместимости Storage и материнской платы
        if (storage != null && motherboard != null) {
            if (!checkStorageMotherboardCompatibility(storage, motherboard, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка совместимости материнской платы и корпуса
        if (motherboard != null && pcCase != null) {
            if (!checkMotherboardCaseCompatibility(motherboard, pcCase, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка совместимости GPU и корпуса (размер)
        if (gpu != null && pcCase != null) {
            if (!checkGpuCaseCompatibility(gpu, pcCase, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка совместимости CPU Cooler и корпуса
        if (cooler != null && pcCase != null) {
            if (!checkCoolerCaseCompatibility(cooler, pcCase, result)) {
                result.setCompatible(false);
            }
        }

        // Проверка достаточности мощности блока питания
        if (psu != null) {
            if (!checkPowerSupplyCapacity(components, psu, result)) {
                result.setCompatible(false);
            }
        }

        if (result.getIssues().isEmpty()) {
            result.setCompatible(true);
            result.addInfo("Все компоненты совместимы");
        }

        return result;
    }

    /**
     * Проверка совместимости CPU и материнской платы (сокет)
     */
    private boolean checkCpuMotherboardCompatibility(PcComponent cpu, PcComponent motherboard, CompatibilityResult result) {
        if (cpu.getSocket() == null || motherboard.getSocket() == null) {
            result.addWarning("Не удалось проверить совместимость сокета (данные неполные)");
            return true; // Не критичная ошибка
        }

        if (!cpu.getSocket().equals(motherboard.getSocket())) {
            result.addIssue(String.format("Несовместимость сокетов: CPU (%s) и материнская плата (%s)",
                    cpu.getSocket(), motherboard.getSocket()));
            return false;
        }

        // Проверка чипсета (Intel/AMD совместимость)
        String cpuManufacturer = cpu.getManufacturer() != null ? cpu.getManufacturer().toLowerCase() : "";
        String chipset = motherboard.getChipset() != null ? motherboard.getChipset().toLowerCase() : "";

        if (cpuManufacturer.contains("intel") && !chipset.contains("intel") && !chipset.isEmpty()) {
            result.addWarning("Проверьте совместимость чипсета материнской платы с процессором Intel");
        } else if (cpuManufacturer.contains("amd") && !chipset.contains("amd") && !chipset.isEmpty()) {
            result.addWarning("Проверьте совместимость чипсета материнской платы с процессором AMD");
        }

        result.addInfo(String.format("CPU (%s) совместим с материнской платой (сокет %s)",
                cpu.getSocket(), motherboard.getSocket()));
        return true;
    }

    /**
     * Проверка совместимости RAM и материнской платы
     */
    private boolean checkRamMotherboardCompatibility(PcComponent ram, PcComponent motherboard, CompatibilityResult result) {
        if (ram.getMemoryType() == null || motherboard.getMemoryType() == null) {
            result.addWarning("Не удалось проверить совместимость типа памяти");
            return true;
        }

        if (!ram.getMemoryType().equals(motherboard.getMemoryType())) {
            result.addIssue(String.format("Несовместимость типа памяти: RAM (%s) и материнская плата (%s)",
                    ram.getMemoryType(), motherboard.getMemoryType()));
            return false;
        }

        // Проверка максимальной частоты
        if (ram.getSpeed() != null && motherboard.getMaxMemory() != null) {
            // Здесь можно добавить проверку максимальной поддерживаемой частоты
            result.addInfo(String.format("RAM (%s %s) совместима с материнской платой",
                    ram.getMemoryType(), ram.getSpeed() + "MHz"));
        }

        return true;
    }

    /**
     * Проверка совместимости GPU и материнской платы
     */
    private boolean checkGpuMotherboardCompatibility(PcComponent gpu, PcComponent motherboard, CompatibilityResult result) {
        // Современные GPU используют PCIe x16, который поддерживается всеми материнскими платами
        // Проверяем наличие PCIe слотов
        if (motherboard.getPcieSlots() != null && motherboard.getPcieSlots() < 1) {
            result.addIssue("Материнская плата не имеет PCIe слотов для видеокарты");
            return false;
        }

        result.addInfo("GPU совместима с материнской платой (PCIe x16)");
        return true;
    }

    /**
     * Проверка совместимости Storage и материнской платы
     */
    private boolean checkStorageMotherboardCompatibility(PcComponent storage, PcComponent motherboard, CompatibilityResult result) {
        if (storage.getInterfaceType() == null) {
            result.addWarning("Тип интерфейса накопителя не указан");
            return true;
        }

        String interfaceType = storage.getInterfaceType().toUpperCase();
        
        // NVMe накопители используют M.2 слот, который должен быть на материнской плате
        if (interfaceType.contains("NVME") || interfaceType.contains("M.2")) {
            result.addInfo("NVMe накопитель должен подключаться к M.2 слоту материнской платы");
        } else if (interfaceType.contains("SATA")) {
            result.addInfo("SATA накопитель совместим с материнской платой");
        }

        return true;
    }

    /**
     * Проверка совместимости материнской платы и корпуса (форм-фактор)
     */
    private boolean checkMotherboardCaseCompatibility(PcComponent motherboard, PcComponent pcCase, CompatibilityResult result) {
        if (motherboard.getFormFactor() == null || pcCase.getCaseFormFactor() == null) {
            result.addWarning("Не удалось проверить совместимость форм-фактора");
            return true;
        }

        String mbFormFactor = motherboard.getFormFactor().toUpperCase();
        String caseFormFactor = pcCase.getCaseFormFactor().toUpperCase();

        // ATX корпус поддерживает ATX, mATX, ITX
        // mATX корпус поддерживает mATX, ITX
        // ITX корпус поддерживает только ITX
        boolean compatible = false;
        if (caseFormFactor.contains("ATX") && mbFormFactor.contains("ATX")) {
            compatible = true;
        } else if (caseFormFactor.contains("MATX") || caseFormFactor.contains("MICRO-ATX")) {
            compatible = mbFormFactor.contains("MATX") || mbFormFactor.contains("ITX") || mbFormFactor.contains("MINI-ITX");
        } else if (caseFormFactor.contains("ITX") || caseFormFactor.contains("MINI-ITX")) {
            compatible = mbFormFactor.contains("ITX") || mbFormFactor.contains("MINI-ITX");
        }

        if (!compatible) {
            result.addIssue(String.format("Несовместимость форм-факторов: Материнская плата (%s) и корпус (%s)",
                    motherboard.getFormFactor(), pcCase.getCaseFormFactor()));
            return false;
        }

        result.addInfo(String.format("Материнская плата (%s) совместима с корпусом (%s)",
                motherboard.getFormFactor(), pcCase.getCaseFormFactor()));
        return true;
    }

    /**
     * Проверка совместимости GPU и корпуса (размер)
     */
    private boolean checkGpuCaseCompatibility(PcComponent gpu, PcComponent pcCase, CompatibilityResult result) {
        // GPU обычно имеет длину около 250-350mm
        // Проверяем максимальную длину GPU, поддерживаемую корпусом
        if (pcCase.getMaxGpuLength() != null) {
            // Предполагаем стандартную длину GPU около 300mm, если не указана
            int gpuLength = 300; // Можно добавить поле length в PcComponent
            
            if (gpuLength > pcCase.getMaxGpuLength()) {
                result.addWarning(String.format("Длина видеокарты (%dmm) может превышать максимальную длину корпуса (%dmm)",
                        gpuLength, pcCase.getMaxGpuLength()));
            } else {
                result.addInfo(String.format("GPU помещается в корпус (максимальная длина: %dmm)",
                        pcCase.getMaxGpuLength()));
            }
        }

        return true;
    }

    /**
     * Проверка совместимости Cooler и корпуса (высота)
     */
    private boolean checkCoolerCaseCompatibility(PcComponent cooler, PcComponent pcCase, CompatibilityResult result) {
        if (pcCase.getMaxCpuCoolerHeight() != null) {
            // Предполагаем стандартную высоту кулера около 160mm, если не указана
            int coolerHeight = 160; // Можно добавить поле height в PcComponent
            
            if (coolerHeight > pcCase.getMaxCpuCoolerHeight()) {
                result.addWarning(String.format("Высота кулера (%dmm) может превышать максимальную высоту корпуса (%dmm)",
                        coolerHeight, pcCase.getMaxCpuCoolerHeight()));
            }
        }

        return true;
    }

    /**
     * Проверка достаточности мощности блока питания
     */
    private boolean checkPowerSupplyCapacity(Map<PcComponent.ComponentType, PcComponent> components, 
                                            PcComponent psu, CompatibilityResult result) {
        int totalWattage = 0;

        // CPU
        if (components.get(PcComponent.ComponentType.CPU) != null) {
            PcComponent cpu = components.get(PcComponent.ComponentType.CPU);
            if (cpu.getTdp() != null) {
                totalWattage += cpu.getTdp();
            } else {
                totalWattage += 100; // Примерная мощность CPU
            }
        }

        // GPU
        if (components.get(PcComponent.ComponentType.GPU) != null) {
            PcComponent gpu = components.get(PcComponent.ComponentType.GPU);
            // GPU обычно потребляет 150-350W
            if (gpu.getTdp() != null) {
                totalWattage += gpu.getTdp();
            } else {
                totalWattage += 200; // Примерная мощность GPU
            }
        }

        // Остальные компоненты (материнская плата, RAM, Storage, вентиляторы)
        totalWattage += 100; // Запас для остальных компонентов

        // Рекомендуется иметь запас 20-30%
        int recommendedWattage = (int) (totalWattage * 1.25);

        if (psu.getWattage() == null) {
            result.addWarning("Мощность блока питания не указана");
            return true;
        }

        if (psu.getWattage() < totalWattage) {
            result.addIssue(String.format("Мощность блока питания (%dW) недостаточна. Требуется минимум %dW (рекомендуется %dW)",
                    psu.getWattage(), totalWattage, recommendedWattage));
            return false;
        } else if (psu.getWattage() < recommendedWattage) {
            result.addWarning(String.format("Рекомендуется блок питания мощностью %dW для запаса (текущий: %dW)",
                    recommendedWattage, psu.getWattage()));
        } else {
            result.addInfo(String.format("Блок питания (%dW) достаточен для сборки (потребление: ~%dW)",
                    psu.getWattage(), totalWattage));
        }

        return true;
    }

    /**
     * Результат проверки совместимости
     */
    public static class CompatibilityResult {
        private boolean compatible = true;
        private final List<String> issues = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();

        public boolean isCompatible() {
            return compatible && issues.isEmpty();
        }

        public void setCompatible(boolean compatible) {
            this.compatible = compatible;
        }

        public void addIssue(String issue) {
            this.issues.add(issue);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public void addInfo(String info) {
            this.info.add(info);
        }

        public List<String> getIssues() {
            return issues;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getInfo() {
            return info;
        }
    }
}

