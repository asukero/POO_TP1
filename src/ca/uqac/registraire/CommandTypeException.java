package ca.uqac.registraire;

// Exception en cas d'erreur de lecture de la commande
public class CommandTypeException extends RuntimeException {
    public CommandTypeException() {
        super();
    }

    public CommandTypeException(String message) {
        super(message);
    }
}
