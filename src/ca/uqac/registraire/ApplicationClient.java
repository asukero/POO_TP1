package ca.uqac.registraire;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ApplicationClient {

    private PrintWriter sortieWriter;
    private BufferedReader commandesReader;
    private InetAddress serverHostname;
    private int portNumber;

    /**
     * prend le fichier contenant la liste des commandes, et le charge dans une
     * variable du type Commande qui est retournée
     */
    public Commande saisisCommande(BufferedReader fichier) {
        try {
            String commandLine = fichier.readLine();
            if (commandLine != null) {
                Commande commande = new Commande(commandLine);
                return commande;
            }
            return null;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());

        } catch (CommandTypeException ex){
            sortieWriter.println(ex.getMessage());
        }
        return null;
    }

    /**
     * initialise : ouvre les différents fichiers de lecture et écriture
     */
    public void initialise(String fichCommandes, String fichSortie) throws Exception {
        commandesReader = new BufferedReader(new FileReader(fichCommandes));
        sortieWriter = new PrintWriter(new FileWriter(fichSortie));
        scenario();
    }

    /**
     * prend une Commande dûment formatée, et la fait exécuter par le serveur. Le résultat de
     * l’exécution est retournée. Si la commande ne retourne pas de résultat, on retourne null.
     * Chaque appel doit ouvrir une connexion, exécuter, et fermer la connexion. Si vous le
     * souhaitez, vous pourriez écrire six fonctions spécialisées, une par type de commande
     * décrit plus haut, qui seront appelées par  traiteCommande(Commande uneCommande)
     */
    public Object traiteCommande(Commande uneCommande) {

        Object reponse = null;
        try{
            Socket clientSocket = new Socket(serverHostname, portNumber);

            ObjectOutputStream outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream inFromServer = new ObjectInputStream(clientSocket.getInputStream());
            outToServer.writeObject(uneCommande);
            reponse = inFromServer.readObject();

            outToServer.flush();
            outToServer.close();
            clientSocket.close();


        }catch (IOException| ClassNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        return reponse;
    }

    /**
     * cette méthode vous sera fournie plus tard. Elle indiquera la séquence d’étapes à exécuter
     * pour le test. Elle fera des appels successifs à saisisCommande(BufferedReader fichier) et
     * traiteCommande(Commande uneCommande).
     */
    public void scenario() {
        sortieWriter.println("Debut des traitements :");
        Commande prochaine = saisisCommande(commandesReader);
        while (prochaine != null) {
            sortieWriter.println("\tTraitement de la commande " + prochaine + " ...");
            Object resultat = traiteCommande(prochaine);
            sortieWriter.println("\t\tResultat: " + resultat);
            prochaine = saisisCommande(commandesReader);
        }
        sortieWriter.println("Fin des traitements");
    }

    /**
     * programme principal. Prend 4 arguments: 1) “hostname” du serveur, 2) numéro de port,
     * 3) nom fichier commandes, et 4) nom fichier sortie. Cette méthode doit créer une
     * instance de la classe ApplicationClient, l’initialiser, puis exécuter le scénario     
     */
    public static void main(String[] args) {
        try {
            if (args.length != 4) {
                throw new IllegalArgumentException("Veuillez indiquer 4 arguments");
            } else {
                ApplicationClient applicationClient = new ApplicationClient();
                applicationClient.serverHostname = InetAddress.getByName(args[0]);
                applicationClient.portNumber = new Integer(args[1]);
                applicationClient.initialise(args[2], args[3]);
                applicationClient.sortieWriter.close();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("Usage:\n" +
                    "\tjava -jar ApplicationClient.jar [hostname] [port number] [command filename] [output filename]\n" +
                    "\tex: java -jar ApplicationClient.jar 192.168.0.1 4242 commandes.txt sortie.txt");
        }

    }
}