package my;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Returns the most frequent term in a list */
public class most_frequent extends DefaultInternalAction {

    @Override
    public Object execute(
        final TransitionSystem ts,
        final Unifier un,
        final Term[] args
    ) throws Exception {
        try {
            if (!args[0].isVar() && !(args[0] instanceof ListTerm)) {
                throw new JasonException(
                    "The argument of the internal action 'most_frequent' must be a list."
                );
            }

            ListTerm list = (ListTerm) args[0];

            Map<Term, Integer> frequencyMap = new HashMap<>();

            for (Term t : list) {
                frequencyMap.put(t, frequencyMap.getOrDefault(t, 0) + 1);
            }

            Term mostFrequentTerm = null;
            int maxCount = -1;

            for (Map.Entry<Term, Integer> entry : frequencyMap.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    mostFrequentTerm = entry.getKey();
                }
            }

            if (mostFrequentTerm != null) {
                return un.unifies(args[1], mostFrequentTerm);
            } else {
                throw new JasonException("No most frequent term found.");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new JasonException(
                "The internal action 'most_frequent' has not received the required argument."
            );
        } catch (Exception e) {
            throw new JasonException(
                "Error in internal action 'most_frequent': " + e,
                e
            );
        }
    }
}
