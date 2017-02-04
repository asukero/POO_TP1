package ca.uqac.registraire;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Commande implements Serializable {

    public enum CommandType {
        COMPILATION,
        CHARGEMENT,
        ECRITURE,
        CREATION,
        FONCTION,
        LECTURE
    }

    private CommandType commandType;

    private HashMap<String, String> attributes = new HashMap<>();

    public Commande(String commandLine) {
        String[] args = commandLine.split("#");
        try {
            switch (args[0]) {
                case "compilation":
                    commandType = CommandType.COMPILATION;
                    attributes.put("chemin_relatif_source", args[1]);
                    attributes.put("chemin_relatif_classe", args[2]);
                    break;
                case "chargement":
                    commandType = CommandType.CHARGEMENT;
                    attributes.put("nom_qualifié_de_classe", args[1]);
                    break;
                case "ecriture":
                    commandType = CommandType.ECRITURE;
                    attributes.put("identificateur", args[1]);
                    attributes.put("nom_attribut", args[2]);
                    attributes.put("valeur", args[3]);
                    break;
                case "creation":
                    commandType = CommandType.CREATION;
                    attributes.put("nom_de_classe", args[1]);
                    attributes.put("identificateur", args[2]);
                    break;
                case "fonction":
                    commandType = CommandType.FONCTION;
                    attributes.put("identificateur", args[1]);
                    attributes.put("nom_fonction", args[2]);
                    if (args.length > 3) {
                        attributes.put("liste_parametres", args[3]);
                    }
                    break;
                case "lecture":
                    commandType = CommandType.LECTURE;
                    attributes.put("nom_qualifié_de_classe", args[1]);
                    break;
                default:
                    throw new CommandTypeException("Type de commande non reconnu");
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new CommandTypeException("La ligne " + commandLine + "ne contient pas assez d'arguments");
        }
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public CommandType getCommandType() {
        return commandType;
    }
}
