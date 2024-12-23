package bgu.spl.mics.application.objects;

/**
 * Represents the status of a system component.
 * Possible statuses:
 * - UP: The component is operational.
 * - DOWN: The component is non-operational.
 * - ERROR: The component has encountered an error.
 */
public enum STATUS {
    UP, DOWN, ERROR;

    /**
     * Converts a string to the corresponding STATUS enum value.
     *
     * @param statusString The status as a string (e.g., "UP", "DOWN", "ERROR").
     * @return The corresponding STATUS enum value, or ERROR if invalid.
     */
    public static STATUS fromString(String statusString) {
        switch (statusString.toUpperCase()) {
            case "UP":
                return UP;
            case "DOWN":
                return DOWN;
            case "ERROR":
                return ERROR;
            default:
            //לא בטוח שצריך
                System.out.println("Invalid status string: " + statusString);
                return ERROR;  // Default to ERROR if invalid
        }
    }

    public static String toString(STATUS status) {
        return status.name();  // Returns the name of the enum (e.g., "UP")
    }
}