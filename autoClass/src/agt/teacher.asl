// Agent alice in project autoClass

/* Initial beliefs and rules */
questions([
    "What is the capital of France?",
    "What is the capital of Italy?",
    "What is the capital of Germany?",
    "What is the capital of Spain?"
]).

current_question(0).
current_iteration(0).
max_iterations(10).

get_question(Q) :-
    current_question(N) & questions(Qs) & .nth(N, Qs, Q).

all_present
  :- partecipantNumber(NP) &
     .count(present[source(_)], NPresent) &
     .count(absent[source(_)], NAbsent) &
     NP = NPresent + NAbsent.

/* Initial goals */

!register.
!start.

/* Plans */

+!register <- .df_register(teacher).

+!start
  <- .print("Starting roll call...");
     !roll_call.

+!roll_call <-
  .wait(2000);
  .df_search("student", Students);
  +partecipantNumber(.length(Students));
  .send(Students, tell, is_present);
  .wait(all_present, 4000, _);
  .findall(A, present[source(A)], PresentStudents);
  .print("Present students: ", PresentStudents);
  !ask_question.

+!ask_question : get_question(Q) <-
  .print("Asking question: ", Q);
  .findall(A, present[source(A)], PresentStudents);
  A \== [];
  .send(PresentStudents, tell, question(Q));
  .wait(2000);
  !collect_answers(Q).


+!collect_answers(Q): current_question(N) & current_iteration(I)
  <- .findall(A, given_answer(Q, A)[source(_)], Answers);
     .print("Answers received: ", Answers);
     my.most_frequent(Answers, Correct);
     .print("Correct answer: ", Correct);
     .findall(A, present[source(A)], PresentStudents);
     .send(PresentStudents, tell, correct(Q, Correct));
     N1 = N + 1;
     I1 = I + 1;
     if(N < 3 & I < 12) {
        .print("Question ", N1, " of 4, iteration ", I1, " of 12");
        -current_question(N);
        +current_question(N1);
        -current_iteration(I);
        +current_iteration(I1);
        .print("\n");
        .wait(2000);
        !ask_question;
    }elif (N == 3 & I < 12) {
        .print("Question ", N1, " of 4, iteration ", I1, " of 12");
        N2 = 0;
        -current_question(N);
        +current_question(N2);
        -current_iteration(I);
        +current_iteration(I1);
        .print("\n");
        .wait(2000);
        !ask_question;
    } else {
        !end;
    }.

+!end : true <-
    .findall(A, present[source(A)], PresentStudents);
    .send(PresentStudents, tell, say_known).
