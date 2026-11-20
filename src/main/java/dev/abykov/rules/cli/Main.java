package dev.abykov.rules.cli;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new RuleEngineCli().run(
                args,
                System.out,
                System.err
        );

        System.exit(exitCode);
    }
}
