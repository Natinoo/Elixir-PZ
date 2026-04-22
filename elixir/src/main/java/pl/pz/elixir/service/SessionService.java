package pl.pz.elixir.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

//dodać symulacje 3 sesji dziennie jak w realu
@Service
public class SessionService {

    private final List<String> currentSession = new ArrayList<>();

    public void addToSession(String paymentJson) {
        currentSession.add(paymentJson);

        System.out.println("Added to session. Current size: " + currentSession.size());
    }

    public void closeSession() {
        System.out.println("=== CLOSING ELIXIR SESSION ===");
        System.out.println("Payments in session: " + currentSession.size());

        // tutaj później netting engine

        currentSession.clear();
    }
}