package org.mobicents.servlet.sip.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.sip.SipServletResponse;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.ServletParseException;

public class Myapp extends SipServlet {

    private static final long serialVersionUID = 1L;
    private static Map<String, String> RegistrarDB = new HashMap<>();
    private static Map<String, String> EstadosDB = new HashMap<>();
    private static SipFactory factory;

    public Myapp() {
        super();
    }

    public void init() {
        factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
    }

     protected void doRegister(SipServletRequest request) throws ServletException,
            IOException {
       
        String to = request.getHeader("To"); // Obtemos o "To" do request
        String aor = getSIPuri(request.getHeader("To")); // Obtemos o "aor" do request

        //int expires = Integer.parseInt(getPortExpires(request.getHeader("Contact"))); // Tranformamos o valor de expires que está em string para int

      //  if (expires != 0) { // Caso o valores "expires" do request seja diferente de 0 (REGISTER)
             doRegistration(request, to, aor); // Efetua o registo

       // }// else { // Caso o valores "expires" do request seja igual a 0 (DEREGISTER)
            //doDeregistration(request, aor); // Efetua o deregisto
        //}
    }

    /**
        * This is the function that actually manages the REGISTER operation
        * @param request The SIP message received by the AS,
        * @param to From the SIP message received,
        * @param aor From the SIP message received
        */
    private void doRegistration(SipServletRequest request, String to, String aor) throws ServletException, IOException {
        SipServletResponse response; // Cria a resposta

        String domain = aor.substring(aor.indexOf("@") + 1, aor.length()); // Obtemos o "domain" do "aor"
        String contact = getSIPuriPort(request.getHeader("Contact")); // Obtemos o "contact" do request

            if ("acme.pt".equals(domain)) { // O dominio corresponde ao pretendido
                RegistrarDB.put(aor, contact); // Adcionamos à BD
                //setStatus(aor, "AVAILABLE"); // Colocamos o está do "aor" com 'AVAILABLE'
                response = request.createResponse(200); // 200 (ok response)
                response.send(); // Envia a mensagem
               
            } else { // O dominio não corresponde ao pretendido
                response = request.createResponse(403); // 403 (forbidden response)
                response.send(); // Envia a mensagem
            }

        // Some logs to show the content of the Registrar database.
        log("----------------------------------------------REGISTER (myapp):----------------------------------------------");
            Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
                    System.out.println(pairs.getKey() + " = " + pairs.getValue());
                }
        log("----------------------------------------------REGISTER (myapp):----------------------------------------------");
    }
        
        /** 
        String aor = getSIPuri(request.getHeader("From"));

        if (validateDomain(aor)) {
            handleRegistration(request, aor);
            logContacts();
        } else {
            request.createResponse(403).send();
        }
    }*/

    protected void doInvite(SipServletRequest request)
            throws ServletException, IOException {
       
        String fromAor = getSIPuri(request.getHeader("From")); // Get the From AoR
        String toAor = getSIPuri(request.getHeader("To")); // Get the To AoR
        String domain = toAor.substring(toAor.indexOf("@")+1, toAor.length());
       
        // Some logs to show the content of the Registrar database.
        log("----------------------------------------------INVITE (myapp):----------------------------------------------");
            Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
                    System.out.println(pairs.getKey() + " = " + pairs.getValue());
                }
        log("----------------------------------------------INVITE (myapp):----------------------------------------------");
       
        if (domain.equals("acme.pt")) { // The To domain is the same as the server
            if (!RegistrarDB.containsKey(fromAor)) { // From AoR not in the database, reply 403
                SipServletResponse response = request.createResponse(403);
                response.send();
            } else if (!RegistrarDB.containsKey(toAor)) { // To AoR not in the database, reply 404
                SipServletResponse response = request.createResponse(404);
                response.send();
            } else {
                    Proxy proxy = request.getProxy();
                    proxy.setRecordRoute(true);
                    proxy.setSupervised(false);
                    URI toContact = factory.createURI(RegistrarDB.get(toAor));
                    proxy.proxyTo(toContact);
                   
            }        

        } else {
            SipServletResponse response = request.createResponse(403);
            response.send();
        }

    }

    protected void doMessage(SipServletRequest request) throws ServletException, IOException {
        String aor_recipiente = request.getContent().toString().trim();

        if (validateMessage(request, aor_recipiente)) {
            request.createResponse(200).send();
        } else {
            sendErrorResponse(request, 403);
        }
    }

    protected void doAck(SipServletRequest request) throws ServletException, IOException {
        updateStatusOnAck(request);
        logContacts();
    }

    protected void doBye(SipServletRequest request) throws ServletException, IOException {
        updateStatusOnBye(request);
        logContacts();
    }

    protected void logContacts() {
        log("***Contacts (myapp):***");
        RegistrarDB.forEach((key, value) -> System.out.println(key + " = " + value));
        EstadosDB.forEach((key, value) -> System.out.println(key + " = " + value));
        log("***Contacts (myapp):***");
    }

    protected String getSIPuri(String uri) {
        String f = uri.substring(uri.indexOf("<") + 1, uri.indexOf(">"));
        int indexColon = f.indexOf(":", f.indexOf("@"));
        if (indexColon != -1) {
            f = f.substring(0, indexColon);
        }
        return f;
    }

    protected boolean validateDomain(String aor) {
        String domain = aor.substring(aor.lastIndexOf("@") + 1);
        return domain.equals("acme.pt");
    }

    protected void handleRegistration(SipServletRequest request, String aor) {
        String contact_field = request.getHeader("Contact");
        String contact = getSIPuriPort(contact_field);
        String expirity = contact_field.substring(contact_field.lastIndexOf("=") + 1);

        if (expirity.equals("0")) {
            RegistrarDB.remove(aor);
            EstadosDB.remove(aor);
            try {
                request.createResponse(200).send();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            RegistrarDB.put(aor, contact);
            EstadosDB.put(aor, "Available");
            try {
                request.createResponse(200).send();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void handleInvite(SipServletRequest request, String aor_recipiente)
            throws TooManyHopsException, ServletParseException, IOException {
        if (aor_recipiente.equals("sip:chat@acme.pt")) {
            handleConferenceInvite(request);
        } else {
            handleNormalInvite(request, aor_recipiente);
        }
    }

    protected void handleConferenceInvite(SipServletRequest request)
            throws TooManyHopsException, ServletParseException, IOException {
        String sala = "sip:conf@127.0.0.1:5070";
        Proxy proxy = request.getProxy();
        proxy.setRecordRoute(true);
        proxy.setSupervised(false);
        URI toContact = factory.createURI(sala);
        proxy.proxyTo(toContact);
    }

    protected void handleNormalInvite(SipServletRequest request, String aor_recipiente)
            throws TooManyHopsException, ServletParseException, IOException {
        if (!RegistrarDB.containsKey(aor_recipiente) || !EstadosDB.get(aor_recipiente).equals("Available")) {
            sendErrorResponse(request, 404);
        } else {
            Proxy proxy = request.getProxy();
            proxy.setRecordRoute(true);
            proxy.setSupervised(false);
            URI toContact = factory.createURI(RegistrarDB.get(aor_recipiente));
            proxy.proxyTo(toContact);
        }
    }

    protected boolean validateMessage(SipServletRequest request, String aor_recipiente) {
        return validateDomain(getSIPuri(request.getHeader("From")))
                && "sip:gofind@acme.pt".equals(request.getHeader("To"))
                && EstadosDB.containsKey(aor_recipiente)
                && EstadosDB.get(aor_recipiente).equals("Available");
    }

    protected void updateStatusOnAck(SipServletRequest request) {
        String aor_remetente = getSIPuri(request.getHeader("From"));
        String aor_recipiente = getSIPuri(request.getHeader("To"));

        if (!aor_recipiente.equals("sip:chat@acme.pt")) {
            EstadosDB.put(aor_remetente, "Busy");
            EstadosDB.put(aor_recipiente, "Busy");
        } else {
            EstadosDB.put(aor_remetente, "In Conference");
        }
    }

    protected void updateStatusOnBye(SipServletRequest request) {
        String aor_remetente = getSIPuri(request.getHeader("From"));
        String aor_recipiente = getSIPuri(request.getHeader("To"));

        if (!aor_recipiente.equals("sip:chat@acme.pt")) {
            EstadosDB.put(aor_remetente, "Available");
            EstadosDB.put(aor_recipiente, "Available");
        } else {
            EstadosDB.put(aor_remetente, "Available");
        }
    }

    protected String getSIPuriPort(String uri) {
        return uri.substring(uri.indexOf("<") + 1, uri.indexOf(">"));
    }

    protected void sendErrorResponse(SipServletRequest request, int statusCode) {
        try {
            request.createResponse(statusCode).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
