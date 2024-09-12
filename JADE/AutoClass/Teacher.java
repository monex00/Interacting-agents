import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class Teacher extends Agent {
    private List<String> questions = Arrays.asList(
        "What is the capital of France?", 
        "What is the capital of Italy?", 
        "What is the capital of Germany?", 
        "What is the capital of Spain?"
    );
    
    private int currentQuestion = 0;
    private int currentIteration = 0;
    private final int maxIterations = 32;

    private Set<AID> presentStudents = new HashSet<>();
    private Set<AID> absentStudents = new HashSet<>();
    
    protected void setup() {
        System.out.println("Teacher agent " + getAID().getName() + " is ready.");
        registerInDF();
        addBehaviour(new StartRollCallBehaviour());
    }

    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("teacher");
            sd.setName(getLocalName());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    
    private class StartRollCallBehaviour extends OneShotBehaviour {
        public void action() {
            System.out.println("Starting roll call...");
            addBehaviour(new SearchForStudentsBehaviour());
        }
    }

    // Appello
    private class SearchForStudentsBehaviour extends OneShotBehaviour {
        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("student");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                System.out.println("Found " + result.length + " students.");
                for (DFAgentDescription dfd : result) {
                    AID student = dfd.getName();
                    ACLMessage rollCall = new ACLMessage(ACLMessage.INFORM);
                    rollCall.setContent("is_present");
                    rollCall.addReceiver(student);
                    send(rollCall);
                }
                addBehaviour(new WaitForRollCallRepliesBehaviour(result.length));
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    // Assente/Presente
    private class WaitForRollCallRepliesBehaviour extends Behaviour {
        private int numStudents;
        private int repliesReceived = 0;

        public WaitForRollCallRepliesBehaviour(int numStudents) {
            this.numStudents = numStudents;
        }

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage reply = receive(mt);
            if (reply != null) {
                repliesReceived++;
                if (reply.getContent().equals("present")) {
                    presentStudents.add(reply.getSender());
                } else {
                    absentStudents.add(reply.getSender());
                }
            } else {
                block();
            }
        }

        public boolean done() {
            return repliesReceived >= numStudents;
        }

        public int onEnd() {
            if(presentStudents.size() == 0) {
                System.out.println("No students present. Terminating.");
                doDelete();
            } else {
                addBehaviour(new AskQuestionBehaviour());
            }
            return super.onEnd();
        }
    }

    // Domande
    private class AskQuestionBehaviour extends OneShotBehaviour {
        public void action() {
           
            if (currentQuestion < questions.size() && currentIteration < maxIterations) {
                doWait(3000);
                System.out.println("\nCurrent question: " + currentQuestion + ", current iteration: " + currentIteration);
                String question = questions.get(currentQuestion);
                System.out.println("Asking question: " + question);
                ACLMessage questionMsg = new ACLMessage(ACLMessage.INFORM);
                questionMsg.setContent(question);
                for (AID student : presentStudents) {
                    questionMsg.addReceiver(student);
                }
                send(questionMsg);
                addBehaviour(new CollectAnswersBehaviour(question));
            } else if(currentIteration < maxIterations) {
                currentQuestion = 0;
                addBehaviour(new AskQuestionBehaviour());
            } else {
                addBehaviour(new EndBehaviour());
            }
            
        }
    }

    // Risposta esatta
    private class CollectAnswersBehaviour extends Behaviour {
        private String question;
        private Map<AID, String> answers = new HashMap<>();
        private int repliesReceived = 0;
    
        public CollectAnswersBehaviour(String question) {
            this.question = question;
        }
    
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage reply = receive(mt);
            if (reply != null) {
                answers.put(reply.getSender(), reply.getContent());
                repliesReceived++;
            } else {
                block();
            }
        }
    
        public boolean done() {
            return repliesReceived >= presentStudents.size();
        }
    
        public int onEnd() {
            System.out.println("Answers received for " + question + ": " + answers.values());
            String correctAnswer = getMostFrequentAnswer(answers);
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("correct:" + question + ":" + correctAnswer);
    
            for (AID student : presentStudents) {
                msg.addReceiver(student);
            }
            send(msg);
    
            currentQuestion++;
            currentIteration++;
            addBehaviour(new AskQuestionBehaviour());
    
            return super.onEnd();
        }
    
        private String getMostFrequentAnswer(Map<AID, String> answers) {
            Map<String, Integer> frequencyMap = new HashMap<>();
    
            for (String answer : answers.values()) {
                frequencyMap.put(answer, frequencyMap.getOrDefault(answer, 0) + 1);
            }
    
            String mostFrequentAnswer = null;
            int maxFrequency = 0;
            for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
                if (entry.getValue() > maxFrequency) {
                    mostFrequentAnswer = entry.getKey();
                    maxFrequency = entry.getValue();
                }
            }
    
            return mostFrequentAnswer;
        }
    }
    

    // Fine sessione
    private class EndBehaviour extends OneShotBehaviour {
        public void action() {
            for (AID student : presentStudents) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("say_known");  
                msg.addReceiver(student);
                send(msg);  
                System.out.println("");

                doWait(1000);
            }
            System.out.println("Session ended.");
            myAgent.doDelete();
        }
    }


    
}
