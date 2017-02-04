package ca.uqac.registraire;

public class CommandTypeException extends RuntimeException {
    public CommandTypeException() {
        super();
    }

    public CommandTypeException(String message) {
        super(message);
    }
}
