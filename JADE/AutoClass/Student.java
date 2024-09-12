import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.*;

public class Student extends Agent {
    private Map<String, List<String>> answers = new HashMap<>();
    private Map<String, String> myAnswers = new HashMap<>(); 
    
    protected void setup() {
        System.out.println("Student agent " + getAID().getName() + " is ready.");
        registerInDF();

        answers.put("What is the capital of France?", Arrays.asList("paris", "paris", "rome"));
        answers.put("What is the capital of Italy?", Arrays.asList("rome", "rome", "paris"));
        answers.put("What is the capital of Germany?", Arrays.asList("berlin", "berlin", "berlin"));
        answers.put("What is the capital of Spain?", Arrays.asList("rome", "madrid", "berlin"));

        addBehaviour(new RespondToRollCallBehaviour());
    }

    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("student");
            sd.setName(getLocalName());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // Appello
    private class RespondToRollCallBehaviour extends Behaviour {
        ACLMessage msg;
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            msg = myAgent.receive(mt);
            if (msg != null && msg.getContent().equals("is_present")) {
                ACLMessage reply = msg.createReply();
                /* if (Math.random() < 0.5) {
                    System.out.println("I'm present.");
                    reply.setContent("present");
                    addBehaviour(new AnswerQuestionBehaviour());
                    // addBehaviour(new CorrectAnswerBehaviour());
                } else {
                    System.out.println("I'm absent.");
                    reply.setContent("absent");
                    myAgent.doDelete();  // Elimina l'agente se assente
                } */
                reply.setContent("present");
                addBehaviour(new AnswerQuestionBehaviour());
                myAgent.send(reply);
            }
        }

        @Override
        public boolean done() {
            return msg != null && msg.getContent().equals("is_present");
        }
    }

    // Domanda e risposta
    private class AnswerQuestionBehaviour extends Behaviour {
        ACLMessage msg;
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            msg = receive(mt);
            if (msg != null && answers.containsKey(msg.getContent())) {
                String question = msg.getContent();
                List<String> possibleAnswers = answers.get(question);
                String chosenAnswer = possibleAnswers.get(new Random().nextInt(possibleAnswers.size())); 

                myAnswers.put(question, chosenAnswer);  

                ACLMessage reply = msg.createReply();
                reply.setContent(chosenAnswer);
                send(reply);
            } else if (msg != null && msg.getContent().equals("say_known")){
                addBehaviour(new SayKnownBehaviour());
             } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return msg != null && (answers.containsKey(msg.getContent()) || msg.getContent().equals("say_known"));
        }

        public int onEnd() {
            addBehaviour(new CorrectAnswerBehaviour());
            return super.onEnd();
        }
    }

    // Correzione
    private class CorrectAnswerBehaviour extends Behaviour {
        ACLMessage msg;
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            msg = receive(mt);
            if (msg != null && msg.getContent().startsWith("correct")) {
                String[] parts = msg.getContent().split(":");
                String question = parts[1].trim();
                String correctAnswer = parts[2].trim();

                if (myAnswers.containsKey(question)) {
                    String givenAnswer = myAnswers.get(question);
                    if (!givenAnswer.equals(correctAnswer)) {
                        System.out.println("Correcting answer for " + question + " from " + givenAnswer + " to " + correctAnswer);
                        //System.out.println("Answers for " + question + " were: " + answers.get(question));
                        List<String> currentAnswers = new ArrayList<>(answers.get(question));
                        currentAnswers.remove(givenAnswer);  
                        currentAnswers.add(0, correctAnswer);
                        answers.put(question, currentAnswers);
                        // System.out.println("Answers for " + question + " are now: " + answers.get(question));
                    } else {
                        System.out.println("Answer for " + question + " is already correct.");
                    }
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return msg != null && msg.getContent().startsWith("correct");
        }

        public int onEnd() {
            addBehaviour(new AnswerQuestionBehaviour());
            return super.onEnd();
        }
    }

    private class SayKnownBehaviour extends OneShotBehaviour {
        public void action() {
            for (Map.Entry<String, List<String>> entry : answers.entrySet()) {
                String question = entry.getKey();
                String randomAnswer = entry.getValue().get(new Random().nextInt(entry.getValue().size()));
                System.out.println("[" + getAID().getName() + "]" + question + ": " + randomAnswer);
            }
            myAgent.doDelete(); 
        }
    }
    
}
