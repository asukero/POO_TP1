package ca.uqac.registraire;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class ApplicationServeur {

    private PrintWriter sortieWriter;
    private ServerSocket serverSocket;
    private String sourcePath;
    private String classPath;

    private Object objectToSendBack = null;

    private HashMap<String, Object> objectsCreated = new HashMap<>();

    private LinkedList<Class> classList = new LinkedList<>();

    private ClassLoader classLoader;

    /**
     * prend le numéro de port, crée un SocketServer sur le port
     */
    public ApplicationServeur(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    /**
     * Se met en attente de connexions des clients. Suite aux connexions, elle lit
     * ce qui est envoyé à travers la Socket, recrée l’objet Commande envoyé par
     * le client, et appellera traiterCommande(Commande uneCommande)     
     */
    public void aVosOrdres() {
        Object clientObject;
        try {


            while (true) {
                Socket connectionSocket = serverSocket.accept();

                ObjectInputStream inFromClient = new ObjectInputStream(connectionSocket.getInputStream());

                ObjectOutputStream outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
                clientObject = inFromClient.readObject();
                Commande commande = (Commande) clientObject;
                traiteCommande(commande);

                if (objectToSendBack != null) {
                    outToClient.writeObject(objectToSendBack);
                    objectToSendBack = null;
                } else {
                    outToClient.writeObject("Le serveur ne renvoie rien pour cette commande");
                }

                outToClient.flush();
                connectionSocket.close();

            }
        } catch (Exception ex) {
            sortieWriter.println("ERROR: " + ex.getMessage());
        }
    }

    /**
     * prend une Commande dument formattée, et la traite. Dépendant du type de commande,
     * elle appelle la méthode spécialisée     
     */
    public void traiteCommande(Commande uneCommande) {
        switch (uneCommande.getCommandType()) {
            case COMPILATION:
                traiterCompilation(uneCommande.getAttributes().get("chemin_relatif_source"), uneCommande.getAttributes().get("chemin_relatif_classe"));
                break;
            case CHARGEMENT:
                traiterChargement(uneCommande.getAttributes().get("nom_qualifié_de_classe"));
                break;
            case CREATION:
                try {
                    Class classACreer = classList.stream().filter(c -> c.getName().equals(uneCommande.getAttributes().get("nom_de_classe"))).findFirst().get();
                    traiterCreation(classACreer, uneCommande.getAttributes().get("identificateur"));
                } catch (NoSuchElementException ex) {
                    objectToSendBack = "ERREUR: La classe " + uneCommande.getAttributes().get("nom_de_classe") + " n'a pas été trouvé sur le serveur";
                    sortieWriter.println(objectToSendBack);
                }
                break;
            case ECRITURE:
                Object objectToWrite = objectsCreated.get(uneCommande.getAttributes().get("identificateur"));

                if (objectToWrite != null) {
                    traiterEcriture(objectToWrite, uneCommande.getAttributes().get("nom_attribut"), uneCommande.getAttributes().get("valeur"));
                } else {
                    objectToSendBack = "ERREUR: L'objet " + uneCommande.getAttributes().get("identificateur") + " n'a pas été trouvé sur le serveur";
                    sortieWriter.println(objectToSendBack);
                }
                break;
            case LECTURE:
                Object objectToRead = objectsCreated.get(uneCommande.getAttributes().get("identificateur"));

                if (objectToRead != null) {
                    traiterLecture(objectToRead, uneCommande.getAttributes().get("nom_qualifié_de_classe"));
                } else {
                    objectToSendBack = "ERREUR: L'objet " + uneCommande.getAttributes().get("identificateur") + " n'a pas été trouvé sur le serveur";
                    sortieWriter.println(objectToSendBack);

                }
                break;
            case FONCTION:
                Object objectToCall = objectsCreated.get(uneCommande.getAttributes().get("identificateur"));

                if (objectToCall != null) {
                    String listeParametres = uneCommande.getAttributes().get("liste_parametres");
                    ArrayList<String> types = new ArrayList<>();
                    ArrayList<String> valeurs = new ArrayList<>();

                    if (!listeParametres.isEmpty()) {
                        String[] parametres = listeParametres.split(",");
                        for (String parametre : parametres) {
                            String[] typeValeur = parametre.split(":");
                            types.add(typeValeur[0]);
                            valeurs.add(typeValeur[1]);
                        }
                    }
                    traiterAppel(objectToCall, uneCommande.getAttributes().get("nom_fonction"), types.toArray(new String[0]), valeurs.toArray(new String[0]));
                } else {
                    objectToSendBack = "ERREUR: L'objet " + uneCommande.getAttributes().get("identificateur") + " n'a pas été trouvé sur le serveur";
                    sortieWriter.println(objectToSendBack);

                }
                break;
            default:
                objectToSendBack = "Erreur: Le type de la commande est inconnue.";
                sortieWriter.println(objectToSendBack);
        }
    }

    /**
     * traiterLecture : traite la lecture d’un attribut. Renvoies le résultat par le
     * socket     
     */
    public void traiterLecture(Object pointeurObjet, String attribut) {
        System.out.println("traiterLecture");
    }

    /**
     * traiterEcriture : traite l’écriture d’un attribut. Confirmes au client que l’écriture
     * s’est faite correctement.     
     */
    public void traiterEcriture(Object pointeurObjet, String attribut, Object valeur) {
        
    }

    /**
     * traiterCreation : traite la création d’un objet. Confirme au client que la création
     * s’est faite correctement.     
     */
    public void traiterCreation(Class classeDeLobjet, String identificateur) {
        try{
            Object createdObject = classeDeLobjet.newInstance();
            objectsCreated.put(identificateur, createdObject);
            objectToSendBack = "OK: L'objet " + identificateur + " de la classe " + classeDeLobjet.getName() + " a été correctement crée.";
            sortieWriter.println(objectToSendBack);
        }catch (InstantiationException | IllegalAccessException ex){
            objectToSendBack = "Erreur: Un problème est survenu lors de l'instanciation de la classe.";
            sortieWriter.println(objectToSendBack);
        }
    }

    /**
     * traiterChargement : traite le chargement d’une classe. Confirmes au client que la création
     * s’est faite correctement.     
     */
    public void traiterChargement(String nomQualifie) {
        try {
            Class classe = classLoader.loadClass(nomQualifie);
            classList.add(classe);
            objectToSendBack = "OK: La classe " + nomQualifie + "a été correctement chargée.";
            sortieWriter.println(objectToSendBack);
        } catch (ClassNotFoundException ex) {
            objectToSendBack = "Erreur: La classe " + nomQualifie + " n'a pas été trouvée.";
            sortieWriter.println(objectToSendBack);
        }

    }

    /**
     * traiterCompilation : traite la compilation d’un fichier source java. Confirme au client
     * que la compilation s’est faite correctement. Le fichier source est donné par son chemin
     * relatif par rapport au chemin des fichiers sources.     
     */
    public void traiterCompilation(String cheminFichierSource, String cheminFichierClass) {

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);


        String[] cheminsFichiers = cheminFichierSource.split(",");
        LinkedList<File> classesToCompile = new LinkedList<>();

        for (String cheminFichier : cheminsFichiers) {
            File file = new File(cheminFichier);
            if (file.exists()) {
                classesToCompile.add(file);
            } else {
                sortieWriter.println("Attention: le fichier " + cheminFichier + " n'a pas été trouvé");
            }

        }

        if (!Files.isDirectory(Paths.get(cheminFichierClass))) {
            sortieWriter.println("Attention: le dossier " + cheminFichierClass + " n'a pas été trouvé, utilisation du chemin par défaut du serveur");
            cheminFichierClass = classPath;
        }

        // This sets up the class path that the compiler will use.
        // I've added the .jar file that contains the DoStuff interface within in it...
        List<String> optionList = new ArrayList<>();
        optionList.add("-g");
        optionList.add("-d");
        optionList.add("./" + cheminFichierClass);


        Iterable<? extends JavaFileObject> compilationUnit
                = fileManager.getJavaFileObjectsFromFiles(classesToCompile);
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                optionList,
                null,
                compilationUnit);

        if (task.call()) {
            /** Load and execute *************************************************************************************************/
            objectToSendBack = "Les fichiers ";
            for (File file : classesToCompile) {
                objectToSendBack = objectToSendBack + file.getPath() + " ";
            }
            objectToSendBack = objectToSendBack + "ont été correctement compilés.";
            sortieWriter.println(objectToSendBack);
            /************************************************************************************************* Load and execute **/
        } else {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                sortieWriter.println("Erreur à la ligne" + diagnostic.getLineNumber() + " de " + diagnostic.getSource().toUri());
            }
        }

        try {
            fileManager.close();
        } catch (IOException e) {
            sortieWriter.println("Erreur: Les fichiers sources ne se sont pas fermés correctement");
        }

    }

    /**
     * traiterAppel : traite l’appel d’une méthode, en prenant comme argument l’objet
     * sur lequel on effectue l’appel, le nom de la fonction à appeler, un tableau de nom de
     * types des arguments, et un tableau d’arguments pour la fonction. Le résultat de la
     * fonction est renvoyé par le serveur au client (ou le message que tout s’est bien
     * passé)     
     */
    public void traiterAppel(Object pointeurObjet, String nomFonction, String[] types,
                             Object[] valeurs) {
        System.out.println("appel");
    }


    /**
     * programme principal. Prend 4 arguments: 1) numéro de port, 2) répertoire source, 3)
     * répertoire classes, et 4) nom du fichier de traces (sortie)
     * Cette méthode doit créer une instance de la classe ApplicationServeur, l’initialiser
     * puis appeler aVosOrdres sur cet objet
     *      
     */
    public static void main(String[] args) {
        try {
            if (args.length != 4) {
                throw new IllegalArgumentException("Veuillez indiquer 4 arguments");
            } else {
                ApplicationServeur applicationServeur = new ApplicationServeur(new Integer(args[0]));

                applicationServeur.sortieWriter = new PrintWriter(args[3]);
                applicationServeur.sortieWriter.println("Le serveur est prêt");
                applicationServeur.sourcePath = args[1];
                applicationServeur.classPath = args[2];

                if (!Files.isDirectory(Paths.get(applicationServeur.sourcePath))) {
                    throw new IOException("Le chemin source n'est pas un dossier ou n'existe pas");
                }

                if (!Files.isDirectory(Paths.get(applicationServeur.classPath))) {
                    throw new IOException("Le chemin classes n'est pas un dossier ou n'existe pas");
                }

                applicationServeur.classLoader = new URLClassLoader(new URL[]{new File(applicationServeur.sourcePath).toURI().toURL()});

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        System.out.println("SERVEUR: interruption recue, fermeture du serveur...");
                        try {
                            applicationServeur.sortieWriter.close();
                            applicationServeur.serverSocket.close();
                        } catch (IOException e) {
                            System.err.println("Erreur lors de la fermeture du socket");
                        }
                    }
                });
                applicationServeur.aVosOrdres();

            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
            System.out.println("Usage:\n" +
                    "\tjava -jar ApplicationServer.jar [port number] [source folder] [class folder] [output filename]\n" +
                    "\tex: java -jar ApplicationServer.jar 4242 ./sources/ ./classes sortieServeur.txt");
        }


    }
}