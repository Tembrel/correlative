package net.peierls.example.correlation;

import java.math.BigInteger;
import java.util.Objects;
import java.util.stream.Stream;
import static java.util.stream.Collectors.joining;


/**
 * A special-purpose bit sequence class that keeps track
 * of rotations and reversals. Includes support for the
 * "correlative" property defined by Michael Hawley: a
 * bit sequence s is correlative if for every r, where r
 * is a cyclic rotation of s or of its reversal, r&s != 0,
 * i.e., s has a bit in common with all of its rotations and
 * reversals of rotations.
 */
public class Bits implements Comparable<Bits> {
    
    public static final Bits EMPTY = Bits.from(0L, 0);
    
    static final BigInteger TWO = BigInteger.valueOf(2L);


    final BigInteger b; // bits
    final int n;        // # bits
    final int r;        // right rotation
    final boolean rev;  // reversed


    private Bits(BigInteger b, int n) {
        this(b, n, 0, false);
    }
    
    private Bits(BigInteger b, int n, int r, boolean rev) {
        this.b = b;
        this.n = n;
        this.r = r;
        this.rev = rev;
    }


    /**
     * Constructs a new bit sequence of length n from
     * the lowest n bits of BigInteger b.
     */
    public static Bits from(BigInteger b, int n) {
        return new Bits(unsign(b, n), n);
    }

    /**
     * Constructs a new bit sequence of length n from
     * the lowest n bits of long b.
     */
    public static Bits from(long b, int n) {
        return from(BigInteger.valueOf(b), n);
    }

    @Override
    public int compareTo(Bits that) {
        int d = this.n - that.n;
        return d == 0 ? this.b.compareTo(that.b) : d;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bits)) return false;
        Bits that = (Bits) o;
        return this.b.equals(that.b) && this.n == that.n;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(b, n);
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = n - 1; i >= 0; --i) {
            buf.append(b.testBit(i) ? "1" : "0");
        }
        if (r != 0 || rev) {
            buf.append(" [")
               .append(rev?"rev":"")
               .append(r==0?"":">>"+r)
               .append("]");
        }
        return buf.toString();
    }
    
    
    public int bitCount() {
        return b.bitCount();
    }
    
    public int bitLength() {
        return b.bitLength();
    }
    
    public BigInteger toBigInteger() {
        return b;
    }
    
    public int size() {
        return n;
    }
    
    public int rotation() {
        return r;
    }
    
    public boolean reversed() {
        return rev;
    }
    
    public Bits clear() {
        return new Bits(b, n);
    }
    
    public String bits() {
        return clear().toString();
    }
    
    public boolean isZero() {
        return b.equals(BigInteger.ZERO);
    }

    public boolean disjoint(Bits that) {
        return this.b.and(that.b).bitLength() == 0;
    }
    
    public Bits concatenate(Bits that) {
        int n = this.n + that.n;
        return new Bits(unsign(this.b.shiftLeft(that.n).or(that.b), n), n);
    }
    
    public Bits reverse() {
        BigInteger c = BigInteger.ZERO;
        for (int i = 0; i < n; ++i) {
            if (b.testBit(i)) {
                c = c.setBit(n - i - 1);
            }
        }
        return new Bits(unsign(c, n), n, (n - r) % n, !rev);
    }
    
    public Bits rotateLeft(int k) {
        return new Bits(unsign(cyclicLeftShift(b, n, k), n), n, (n + r - 1) % n, rev);
    }
    
    public Bits rotateRight(int k) {
        return new Bits(unsign(cyclicRightShift(b, n, k), n), n, (r + 1) % n, rev);
    }

    
    Stream<Bits> rotations() {
        return Stream.iterate(this, b -> b.rotateRight(1)).limit(n);
    }
    
    Stream<Bits> rotationsAndReversals() {
        return Stream.concat(rotations().skip(1), reverse().rotations());
    }

    public boolean correlative() {
        return !isZero() 
            && rotationsAndReversals().allMatch(b -> !this.disjoint(b));
    }
    
    public Bits nonCorrelate() {
        return rotationsAndReversals()
            .filter(b -> disjoint(b))
            .findFirst()
            .orElse(Bits.EMPTY);
    }
    
    public String justification() {
        if (correlative()) {
            return rotationsAndReversals()
                .map(r -> String.format("%s%n%s OK%n", this, r))
                .collect(joining("\n"))
                ;
        } else {
            return String.format("%s%n%s NOT OK%n", this, nonCorrelate());
        }
    }

    public static Stream<Bits> correlative(int n, int k) {
        switch (n) {
            case 0:
                return Stream.empty();
            case 1:
                return Stream.<Bits>builder().add(Bits.from(1L, 1)).build();
            default:
                BigInteger start = allOnes(n/2);
                BigInteger end   = allOnes(Math.min(n/2 + k, n));
                //System.out.printf("n=%d, k=%d, start=%s, end=%s%n", n, k, start, end);
                return Stream.iterate(start, i -> i.add(TWO))
                             .limit(1 + end.subtract(start).longValueExact()/2)
                             //.filter(i -> i.bitCount() >= n/4)
                             .map(i -> Bits.from(i, n))
                             .filter(Bits::correlative)
                             ;
        }
    }


    /*
     * Bit-manipulation stuff cribbed from some StackOverflow answer
     */
    
    // Use rotations to prevent getting negatives.
    static BigInteger unsign(BigInteger n, int L) {
        return n.andNot(BigInteger.valueOf(-1).shiftLeft(L));
    }

    static BigInteger cyclicLeftShift(BigInteger n, int L, int k) {
        return n.shiftLeft(k)
                .or(n.shiftRight(L - k))
                .and(allOnes(L));
    }

    private BigInteger cyclicRightShift(BigInteger n, int L, int k){
        //return (n.shiftRight(k).or(n.shiftLeft(L-k))).and(allOnes(L));
        return cyclicLeftShift(n, L, L-k);
    }

    static BigInteger allOnes(int L) {
        return BigInteger.ZERO
                .setBit(L)
                .subtract(BigInteger.ONE);
    }
}