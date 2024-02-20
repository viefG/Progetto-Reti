package org.example;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {



    Socket mioSocket = null;

    int porta = 6789;  //Porta del server

    PrintWriter out;
    BufferedReader in;

    Scanner scanner = new Scanner(System.in);

    BufferedReader tastiera;

    public void comunicazioneIniziale() { // Questo metodo si occupa di gestire l'accesso del client, se tramite login (utente registrato o tramite anon)
        System.out.print("Come vuoi accedere? Anon, Login ");
        String richiesta = scanner.nextLine();
        String lowerRichiesta=richiesta.toLowerCase(); // Ho utilizzato toLowerCase per evitare problemi quando l'utente scrive lettere maiuscole in modo da gestire anche i caratteri in maiuscolo
        if (!lowerRichiesta.equals("login") && !lowerRichiesta.equals("anon")){
            System.out.println("Comando sconosciuto");
            comunicazioneIniziale();
        }
        System.out.println("Invio: " + richiesta);

        switch (lowerRichiesta){
            case "login" :
                gestisciLogin(richiesta); // Chiama il metodo che gestisce il login come utente registrato (in questo caso come in tutti gli altri si passa anche la richiesta al metodo che poi verra' passata anche al client
                break;
            case "anon" :
                gestisciAnon(richiesta);// Chiama il metodo che gestisce il login come anonimo
                break;
        }
    }

    public void comunica() { // Questo metodo e' il cuore del client, gestisce tutte le richieste che vanno mandate al server, ogni volta che si vuole fare una nuova richiesta si passa da questo metodo
        try {
            String richiesta = in.readLine();
            System.out.println(richiesta);
            String risposta=scanner.nextLine();
            String lowerRichiesta=risposta.toLowerCase();
            if (!lowerRichiesta.equals("list") && !lowerRichiesta.equals("download") && !lowerRichiesta.equals("rename") && !lowerRichiesta.equals("delete") && !lowerRichiesta.equals("logout") && !lowerRichiesta.equals("upload"))  { // Questo if server per assicurasi di gestire solo le richieste previste, nel caso in cui la richiesta non si tra quelle gestite in questo ciclo if, il client restitusce che il comando e' sconosciuto e chiede un nuovo comando
                System.out.println("Comando sconosciuto");
                out.println("errore");
                comunica(); // Se il comando inserito non e' presente nei parametri dell'if, il client richiama il metodo comunica per riporvare
            }
            switch (lowerRichiesta){ // Questo switch case serve per lanciare i metodi che gestiranno le richieste del client da mandare al server
                case "list" :
                    stampaLista(lowerRichiesta);
                    break;
                case "download" :
                    scarica(lowerRichiesta);
                    break;
                case "upload" :
                    upload(lowerRichiesta);
                    break;
                case "rename" :
                    rinomina(lowerRichiesta);
                    break;
                case "delete":
                    cancella(lowerRichiesta);
                    break;
                case "logout":
                    disconnetti(lowerRichiesta);
                    break;
            }
        } catch (IOException e){
            System.out.println("Errore "+e.getMessage());
        }

    }


    public Socket connetti(){ // Questo metodo serve per far connettere il client al server
        try {
            System.out.println("provo a connettermi");
            this.mioSocket = new Socket(InetAddress.getLocalHost(), porta);
            System.out.println("Sono connesso");
            in = new BufferedReader(new InputStreamReader(mioSocket.getInputStream()));
            out = new PrintWriter(mioSocket.getOutputStream(), true);
            comunicazioneIniziale();
        } catch (IOException e){
            System.out.println("errore");
        }
        return mioSocket;
    }

    public void gestisciLogin(String richiesta){ // Questo metodo server per mandare la richiesta di login come utente registrato al server, inviando anche il nome e la password
        try {
            out.println(richiesta);
            System.out.println("Inserisci nome");
            String nome= scanner.nextLine();
            System.out.println("Inserisci password");
            String pass= scanner.nextLine();
            out.println(nome);
            out.println(pass);


            String risultato=in.readLine();


            if (risultato.equals("password errata")){
                System.out.println(risultato);
                comunicazioneIniziale();
            } else if (risultato.equals("Utente non trovato")){
                System.out.println(risultato);
                comunicazioneIniziale();
            }
            System.out.println(risultato);


        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        comunica();
    }

    public void gestisciAnon(String richiesta){ // Questo metodo serve per richiede l'accesso come utente anonimo al server
        out.println(richiesta);
        try {
            String risposta=in.readLine();
            System.out.println(risposta);
        } catch (IOException e){
            System.out.println("errore "+e.getMessage());
        }
        comunica();
    }

    public void stampaLista(String richiesta){ // Questo metodo server per richiede al server la lista di comandi utulizzabile dall'uteente dal quale si e' effettuato l'accesso
        out.println(richiesta); // Invia la richiesta al server

        try{
            while (true) {
                String risultato = in.readLine();
                if (risultato.equals("END")) {
                    System.out.println("Cartella non presente, contattare assistenza");
                    break;
                }
                if (!risultato.equals("finito")){
                    System.out.println(risultato);
                } else if (risultato.equals("finito")){
                    comunica();
                }

            }
        } catch (IOException e){
            System.out.println("errore"+e.getMessage());
        }
        System.out.println("sono qua");

    }

    public void scarica(String richiesta) { // Questo metodo permette di scaricare i file dal server al disco del client, per farlo e' stata creata una nuova socket per lo scambio di dati
        String posizione= "./Download/";
        try {
            out.println(richiesta);
            System.out.println(in.readLine());
            String nomeFile = scanner.nextLine();
            posizione=posizione+nomeFile;
            out.println(nomeFile);


            int portaDati = Integer.parseInt(in.readLine()); // Aspetta la risposta del server che include la porta per la connessione dati

            // Stabilisce una nuova connessione Socket per permettere il trasferimento dei dati tra il client ed il server
            try (Socket socketDati = new Socket(mioSocket.getInetAddress(), portaDati);
                 InputStream is = socketDati.getInputStream();
                 FileOutputStream fos = new FileOutputStream(posizione);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) > 0) {
                    bos.write(buffer, 0, bytesRead);
                }
                System.out.println("File " + nomeFile + " scaricato con successo.");
            } catch (IOException e) {
                System.out.println("Errore durante il download del file: " + e.getMessage());
            }

            comunica(); // Torna alla comunicazione normale dopo il download
        } catch (IOException e) {
            System.out.println("Errore durante la comunicazione con il server: " + e.getMessage());
        }
        comunica();
    }

    public void upload(String richiesta){ // Questo metodo permette al client nel caso in cui abbia effettuato l'accesso come utente di caricare file sul server
        try {

            out.println(richiesta);
            String posizione="./DiscoDelClient/";
            System.out.println(in.readLine());
            String nome=scanner.nextLine();
            posizione=posizione+nome;
            out.println(nome);


            int portaDati = Integer.parseInt(in.readLine());

            // Connetti alla porta dati e invia il file
            try (Socket socketDati = new Socket(mioSocket.getInetAddress(), portaDati);
                 BufferedOutputStream bos = new BufferedOutputStream(socketDati.getOutputStream());
                 FileInputStream fis = new FileInputStream(posizione)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
            }
            System.out.println("File caricato con successo.");
        } catch (IOException e) {
            System.out.println("Errore durante l'upload del file: " + e.getMessage());
        }
        comunica();
    }

    public void rinomina(String richiesta){ // Questo metodo serve per rinominare un file presente sul server (solo gli utenti registrati posso utilizzare questo metodo)
        try {
            out.println(richiesta);
            System.out.println(in.readLine());
            String nomeFile = scanner.nextLine();
            out.println(nomeFile);
            System.out.println(in.readLine());
            String nuovoNome=scanner.nextLine();
            out.println(nuovoNome);
            String risultato=in.readLine();
            System.out.println(risultato);

        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        comunica();
    }

    public void cancella(String richiesta){ // QUesto metodo permette di eliminare un file presente sul server (Solo gli utenti registrati posso usare questo metodo)
        try {
            out.println(richiesta);
            System.out.println(in.readLine());
            String nome=scanner.nextLine();
            out.println(nome);
            System.out.println(in.readLine());
            comunica();
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    public void disconnetti (String richiesta){ // Questo metodo permette la disconnessione dell'utente, richiamando il metodo comunicazioneIniziale per dare la possibilita' di effettuare l'accesso come anonimo o come un altro utente
        out.println(richiesta);
        try {
            System.out.println(in.readLine());
            comunicazioneIniziale();
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }



    public static void main(String[] args) { // Il metodo main lancia il metodo connetti per avviare la socket
        Client c = new Client();
        c.connetti();
    }
}