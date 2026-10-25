package com.eqe.hl7workbench;

/** Application entry point. UI behaviour is implemented by {@link MainWindow}. */
public final class HL7Workbench {
    private HL7Workbench() {
    }

    public static void main(String[] args) {
        MainWindow.open();
    }
}
