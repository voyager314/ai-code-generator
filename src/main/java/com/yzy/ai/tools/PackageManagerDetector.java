package com.yzy.ai.tools;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PackageManagerDetector {

    private static final PackageManager[] DETECTION_ORDER = {
            PackageManager.BUN,
            PackageManager.PNPM,
            PackageManager.YARN,
            PackageManager.DENO,
            PackageManager.NPM
    };

    public PackageManager detect(Path workspaceRoot) {
        for (PackageManager pm : DETECTION_ORDER) {
            if (Files.exists(workspaceRoot.resolve(pm.getLockfileName()))) {
                return pm;
            }
        }
        return PackageManager.NPM;
    }
}
