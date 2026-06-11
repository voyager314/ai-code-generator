package com.yzy.ai.tools;

import lombok.Getter;

@Getter
public enum PackageManager {

    NPM("npm", "package-lock.json", "npm install", "npm run", "npm install", "--save-dev"),
    YARN("yarn", "yarn.lock", "yarn install", "yarn", "yarn add", "-D"),
    PNPM("pnpm", "pnpm-lock.yaml", "pnpm install", "pnpm", "pnpm add", "-D"),
    BUN("bun", "bun.lockb", "bun install", "bun run", "bun add", "-d"),
    DENO("deno", "deno.lock", "deno install", "deno task", "deno add", "--dev");

    private final String displayName;
    private final String lockfileName;
    private final String installCommand;
    private final String runPrefix;
    private final String addCommand;
    private final String devFlag;

    PackageManager(String displayName, String lockfileName, String installCommand,
                   String runPrefix, String addCommand, String devFlag) {
        this.displayName = displayName;
        this.lockfileName = lockfileName;
        this.installCommand = installCommand;
        this.runPrefix = runPrefix;
        this.addCommand = addCommand;
        this.devFlag = devFlag;
    }

    public String getAddCommandWithDev(boolean isDev) {
        if (isDev) {
            return addCommand + " " + devFlag;
        }
        return addCommand;
    }
}
