package org.example;

import com.sun.source.tree.TryTree;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server { // Classe server
    ServerSocket server = null;
    Socket socketClient = null;

    int porta = 6789;  //Porta resa disponibile per la comunicazione con il client

    PrintWriter out; //Permette la comunicazione di output con il client

    BufferedReader in; // Permette la comunicazione di input con il client

    String nomeUtente;

    String posizioneServer= System.getProperty("user.dir");

    int root = 5; // Gestisce i permessi tra Utente registrato (vale 2) ed utente Anonimo (vale 0)


    public Socket attendi(){ // Questo metodo serve per mettere il server in ascolto, aspetta fino a quando un client non si collega sulla porta
        try {
            System.out.println("Server On") ;
            server = new ServerSocket(porta); // inizializzazione del servizio
            System.out.println("Server in ascolto sulla porta: "+porta);
            socketClient = server.accept();
            System.out.println("Connessione stabilita");
            in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            out = new PrintWriter(socketClient.getOutputStream(), true);
            accedi();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return socketClient;


    }

    public void accedi(){ //Questo metodo gescisce il tipo di accesso, Se anon chiama il metodo apposito, se invece e' login (cioe' utente regisrato) chiama il metodo di login
        try {
            String accesso=in.readLine();
            String accessoLower = accesso.toLowerCase();
            switch (accessoLower){
                case "anon":
                    gestisciAnon();
                    break;
                case "login":
                    controlloLogin();
                    break;
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        comunica();
    }

    public void comunica(){ // Questo metodo e' il cuore del server, mette a disposizione del client la lista dei comandi ed in base ai comandi che gli vengono restituiti chiama i metodi per gestire le richieste
        try {
            switch (root){
                case 2:
                    out.println("Inserisci comando: Download, Upload, List, Rename,Delete,Logout"); // se root vale 2 (utente registrato) da la lista dei comandi per gli utenti registrati
                    break;
                case 0:
                    out.println("Inserisci comando: List, Download,Logout"); // se root vale 0 da la lista apposita per gli utenti non registrati
                    break;
            }

            String richiesta = in.readLine();
            if (richiesta.equals("errore")){
                comunica();
            }
            String LowerRichiesta=richiesta.toLowerCase();
            System.out.println("test"+LowerRichiesta);


            switch (LowerRichiesta){ // Questo switch case lancia i metodi per gestire le richieste del client
                case "download" :
                    scarica();
                    break;
                case "upload" :
                    upload();
                    break;
                case "list" :
                    stampaLista();
                    break;
                case "rename" :
                    rinomina();
                    break;
                case "delete":
                    cancella();
                    break;
                case "logout":
                    disconnetti();
                    break;
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }

    }

    public void gestisciAnon(){ // setta il root a 0 e rilancia il cuore del server (comunica)
        root=0;
        out.println("accesso come Anonimo effettuato con successo");
        comunica();
    }


    public void controlloLogin(){ // Questo metodo legge dalla cartella del server: DataBase il nome del file .txt per controllare il nome ed il contenuto del file per controllare la password, se sono entrambi corretti setta il root a 2
        try {

            String posizione=posizioneServer+"/DataBase/";


            String nome= in.readLine();
            String pass = in.readLine();
            String posizione2=posizione+nome+"//"+nome+".txt";
            System.out.println(posizione2);

            File fileUtente = new File(posizione2);

            if (!fileUtente.exists()) { // Verifica se il file (utente) esiste
                System.out.println("Utente non esistente.");
                out.println("Utente non trovato");
                accedi();
            }

            try (BufferedReader lettore = new BufferedReader(new FileReader(fileUtente))){
                String password = lettore.readLine();
                if (password.equals(pass)){
                    root=2;
                    out.println("ciao "+nome+" ti sei autentificato con successo");
                    nomeUtente=nome;
                    comunica();
                } else {
                    out.println("password errata");
                    accedi();
                }
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }


        System.out.println("autenticazione successo");


    }

    public void scarica() { // Questo metodo gescisce le richieste di download, crea una nuova socket per permettere il download dei file, i file vengono scaricati dal server alla cartella del client
        String nomeFile = null;
        String posizione="";
         if (root == 2){
              posizione="DataBase//"+nomeUtente+"//cartella";
         } else if (root==0){
              posizione="DataBase//CartellaAnon";
         }

        try {
            out.println("Quale file intendi scaricare?");
            nomeFile = in.readLine();
            posizione=posizione+"/"+nomeFile;
            if (nomeFile == null || nomeFile.trim().isEmpty()) {
                out.println("File non trovato");
                comunica();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        File file = new File(posizione);
        if (!file.exists()) {
            out.println("File non trovato");
            comunica();
        } else {
            out.println("File trovato");
            System.out.println("File trovato");
        }

        try (ServerSocket serverSocketDati = new ServerSocket(0)) { // 0 per selezionare una porta disponibile automaticamente
            int portaDati = serverSocketDati.getLocalPort();
            out.println(portaDati); // Comunica al client la porta per la connessione dati

            try (Socket socketDati = serverSocketDati.accept();
                 FileInputStream fis = new FileInputStream(file);
                 BufferedOutputStream bos = new BufferedOutputStream(socketDati.getOutputStream())) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    bos.write(buffer, 0, count);
                }
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        comunica();
    }

    public void upload(){ // Questo metodo e' l'inverso del metodo download, permette l'upload dei file dal client al server
        try {
            String posizione="DataBase//"+nomeUtente+"//cartella//";
            out.println("Inserisci il nome del file che intendi mandare al server");
            String nome=in.readLine();
            posizione=posizione+nome;
            if (in.readLine().equals("File non trovato")){
                System.out.println("File non trovato");
                comunica();
            }

            try (ServerSocket serverSocketDati = new ServerSocket(0)) {
                int portaDati = serverSocketDati.getLocalPort();
                out.println(portaDati); // Invia la porta al client

                try (Socket socketDati = serverSocketDati.accept();
                     BufferedInputStream bis = new BufferedInputStream(socketDati.getInputStream());
                     FileOutputStream fos = new FileOutputStream(posizione)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                System.out.println("Errore durante la ricezione del file: " + e.getMessage());
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        comunica();
    }


    public void stampaLista(){ // Questo metodo permette di stampare la lista dei comandi in base al tipo di accesso effettuato
        String posizione = "";
        switch (root){
            case 0 :
                System.out.println("sono anonimo");
                posizione="./DataBase//CartellaAnon";
                break;
            case 2 :
                System.out.println("ciao "+nomeUtente+"questa è la tua cartella");
                posizione="DataBase//"+nomeUtente+"//cartella";
                System.out.println(posizione);
                break;
        }

        File directory = new File(posizione);
        System.out.println(directory.getAbsolutePath());
        File[] filesList = directory.listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                if (file.isFile()) {
                    System.out.println("File: " + file.getName());
                    out.println("File: " + file.getName());
                }
            }
        } else {
            System.out.println("Cartella non trovata");
            out.println("END");
        }
        out.println("finito");
        comunica();
    }

    public void rinomina(){ // Questo metodo permette di rinominare il nome dei file gia' presenti sul server
        String posizione="DataBase//"+nomeUtente+"//cartella";


        try {
            out.println("Quale file vuoi rinominare??");
            String nomeFile=in.readLine();
            out.println("Inserisci il nuovo nome");
            String nuovoNome=in.readLine();
            File fileEsistente = new File(posizione+"/"+nomeFile);
            System.out.println(posizione+nomeFile);

            // Crea un oggetto File per il nuovo nome del file
            File nuovoFile = new File(posizione+"/"+nuovoNome);

            // Rinomina il file
            boolean successo = fileEsistente.renameTo(nuovoFile);

            if (successo) {
                out.println(nomeFile+" rinominato con successo");
            } else {
                out.println("impossibile rinominare "+nomeFile);
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }

        comunica();
    }

    public void cancella(){ // Questo metodo permette di cancellare i file presenti sul server
        System.out.println("sono qua");
        out.println("Quale file vuoi cancellare??");
        String posizione="DataBase//"+nomeUtente+"//cartella";
        try {
            String nome=in.readLine();
            posizione=posizione+"/"+nome;
            File fileDaEliminare=new File(posizione);
            if (fileDaEliminare.delete()){
                out.println("File eliminato correttamente");
                comunica();
            } else {
                out.println("non e' stato possibile eliminare il file");
                comunica();
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }

    }

    public void disconnetti(){ // Questo metodo permette la disconnessione degli utenti, richiamando alla fine il metodo accedi per poter permettere un nuovo accesso
        root=5;
        nomeUtente="";
        out.println("Disconnessione effettuata con successo");
        accedi();
    }


    public static void main(String[] args) { // Il main lancia solo il metodo accedi che avvia il tutto
        Server s = new Server();
        s.attendi();
    }
}
