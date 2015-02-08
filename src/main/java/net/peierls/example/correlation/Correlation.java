package net.peierls.example.correlation;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Tests to determine whether correlative property C holds 
 * under concatenation, i.e., whether C(a), C(b) => C(a.b). 
 * Spoiler: It doesn't.
 */
public class Correlation {

    /**
     * Lexicographically minimal correlative sequences of various lengths.
     */
    static void findMins(int N_LO, int N_HI) {
        IntStream.rangeClosed(N_LO, N_HI).forEach(n -> {
            System.out.printf("min(%d) = %s%n",
                n, Bits.correlative(n, 1).min((a, b) -> a.compareTo(b)).orElse(Bits.EMPTY)
            );
        });
    }

    static class Pair {
        final Bits a;
        final Bits b;
        Pair(Bits a, Bits b) { this.a = a; this.b = b; }
    }

    /**
     * Counterexamples to the proposal that C holds under concatenation:
     * C(a), C(b) => C(a.b). Limited to sequences within the given length
     * bounds and only examining sequences of length n with highest bit set
     * being n/2 + k or lower, i.e., with an initial sequence of zeros of
     * length at least n - (n/2 + k).
     */
    static Stream<Pair> counterExamples(int M_LO, int M_HI, int N_LO, int N_HI, int k) {
        return IntStream.rangeClosed(M_LO, M_HI).boxed().flatMap(m ->
            Bits.correlative(m, k).flatMap(a -> 
                IntStream.rangeClosed(N_LO, N_HI).boxed().flatMap(n -> 
                    Bits.correlative(n, k)
                        .filter(b -> !a.concatenate(b).correlative())
                        .map(b -> new Pair(a, b))
                )
            )
        );
    }

    /**
     * Finds all counterexamples within the given ranges.
     */
    static void findCounterExamples(int M_LO, int M_HI, int N_LO, int N_HI, int k) {
        counterExamples(M_LO, M_HI, N_LO, N_HI, k)
            .forEach(p -> {
                Bits a = p.a;
                Bits b = p.b;
                System.out.printf("a = %s (%d), b = %s (%d), c'= %s%n", 
                    a, a.size(), b, b.size(), a.concatenate(b).nonCorrelate());
            });
    }
    
    /**
     * Finds first counterexample within the given ranges, and justifies that
     * counterexample.
     */
    static void findFirstCounterExample(int M_LO, int M_HI, int N_LO, int N_HI, int k) {
        Optional<Pair> found = counterExamples(M_LO, M_HI, N_LO, N_HI, k).findFirst();
        if (found.isPresent()) {
            Bits a = found.get().a;
            Bits b = found.get().b;
            Bits c = a.concatenate(b);
            Bits nC = c.nonCorrelate();
            System.out.printf("a.b = %s.%s%nc'  = %s%n%nJustification%n%n", a, b, nC);
            System.out.printf("a%n%s%n", a.justification());
            System.out.printf("b%n%s%n", b.justification());
            System.out.printf("a.b%n%s%n", c.justification());
        } else {
            System.out.printf("Not found");
        }
    }
    
    
    public static void main(String... args) {
        findMins(1, 30);
        findFirstCounterExample(1, 1, 1, 9, 3);
        findCounterExamples(1, 1, 1, 9, 100);
    }
}
