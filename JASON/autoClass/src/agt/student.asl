// Agent bob in project autoClass

/* Initial beliefs and rules */

answer("What is the capital of France?", [paris, paris, rome]). /* P(paris) = 2/3, P(rome) = 2/3 */
answer("What is the capital of Italy?", [rome, rome, paris]).
answer("What is the capital of Germany?", [berlin, berlin, berlin]).
answer("What is the capital of Spain?", [rome, madrid, berlin]).

/* Initial goals */

// !correct("What is the capital of France?", rome, paris).
// !give_answer("What is the capital of France?").

!register.
/* Plans */

+!register
  <- .df_register("student");
     .df_subscribe("teacher").


+is_present[source(A)]: true <-
    .random(X);
    //.send(A, tell, present).

    if (X < 0.5) {
        .send(A, tell, present);
    } else {
        .send(A, tell, absent);
    }.


+question(Q)[source(A)]: true <-
    !give_answer(Q, A).


+!give_answer(Q, T) : answer(Q, A) <-
    .random(A, X);
    // .print(Q,X);
    +myAnswer(Q, A, X);
    .send(T, tell, given_answer(Q, X)).

+correct(Q, Correct)[source(A)]: answer(Q, Ans) & myAnswer(Q, Ans, X)  <-
    -myAnswer(Q, Ans, X);
    if (not(Correct = X)) {
        !correctMyAns(Q, X, Correct);
    }.


+!correctMyAns(Q, MA, CA) : answer(Q, A) <-
    -answer(Q, A);
    .delete(MA, A, L);
    .concat([CA], L, L1);
    +answer(Q, L1).
    // .print("Corrected answer for ", Q, " from ", MA, " to ", CA).

+say_known: true <-
    .findall([Q, A], answer(Q, A), L);
    !printKnown(L).

+!printKnown([]) <- true.

+!printKnown([[Q, A] | T]) <-
    .random(A, X);
    .print("I know that ", Q, " is ", X);
    !printKnown(T).
