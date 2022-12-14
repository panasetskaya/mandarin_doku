// This class is copyrighted in 2018 by Colin Smith, MIT License: https://github.com/littleredcomputer

package com.panasetskaia.charactersudoku.data.gameGenerator;


import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * An implementation of Knuth's Dancing Links algorithm from pre-fascicle 5C
 * of TAOCP volume 4.
 */
public class ExactCoverProblem {
    private static final Splitter colonSplitter = Splitter.on(':').omitEmptyStrings();

    public enum Strategy {
        FIRST,
        MRV,
    }
    private final Strategy strategy = Strategy.MRV;

    private static class Option {
        Option(int item, int color) { this.item = item; this.color = color; }
        int item;
        int color;
    }

    private final List<String> items = new ArrayList<>();
    private final List<List<Option>> options = new ArrayList<>();  // integer to option == subset of item indices
    private final List<String> colors = new ArrayList<>();  // integer to color (one-based)
    private final Map<String, Integer> itemIndex = new HashMap<>();  // inverse of above mapping
    private final Map<String, Integer> colorIndex = new HashMap<>();

    private int N = 0;  // N and N1 are as in Knuth
    private int N1 = 0;  // index of last primary item (== N if there are no secondary items)

    private ExactCoverProblem() {
        // Create a dummy item and color, so that the indices to these are one-based
        items.add("*UNUSED*");
        colors.add("*UNUSED*");
    }

    private void addItem(String item) {
        if (item.equals(";")) {
            // The ; switches us from recording primary to secondary items.
            if (N == 0) throw new IllegalArgumentException("There must be at least one primary item");
            if (N1 != 0) throw new IllegalArgumentException("Tertiary items are not supported");
            N1 = N;
            return;
        }
        if (itemIndex.containsKey(item)) throw new IllegalArgumentException("duplicate item: " + item);
        itemIndex.put(item, items.size());
        items.add(item);
        ++N;
    }


    public Stream<List<Integer>> solutions() {
        return StreamSupport.stream(new Solutions(), false);
    }

    public List<List<String>> optionsToItems(List<Integer> options) {
        return options.stream().map(i ->
                this.options.get(i).stream().map(o -> items.get(o.item)).collect(Collectors.toList())).collect(Collectors.toList());
    }

    private int colorToNumber(String color) {
        Integer i = colorIndex.get(color);
        if (i != null) return i;
        int index = colors.size();
        colors.add(color);
        colorIndex.put(color, index);
        return index;
    }

    /**
     * Add an option (nonempty subset of established items). Empty options or options
     * continaing a repeated item will throw IllegalArgumentException.
     * Referring to an unknown
     * option will throw. The colon character is special: An option given in the
     * form o:c refers to item o with color c.
     * @param items sequence of item names
     */
    private void addOption(Iterable<String> items) {
        Set<Integer> itemsSeen = new HashSet<>();
        List<Option> itemsOfOption = new ArrayList<>();

        for (String itemcolor : items) {
            List<String> oc = colonSplitter.splitToList(itemcolor);
            if (oc.size() < 1 || oc.size() > 2) throw new IllegalArgumentException("malformed option " + itemcolor);
            String item = oc.get(0);
            Integer ix = itemIndex.get(item);
            if (ix == null) throw new IllegalArgumentException("unknown item: " + item);
            if (itemsSeen.contains(ix)) throw new IllegalArgumentException("item repeated in option: " + item);
            itemsOfOption.add(new Option(ix, oc.size() > 1 ? colorToNumber(oc.get(1)) : 0));
            itemsSeen.add(ix);
        }
        if (itemsOfOption.isEmpty()) return;
        options.add(itemsOfOption);
    }

    private class Solutions implements Spliterator<List<Integer>> {
        private final int[] llink = new int[items.size() + 1];
        private final int[] rlink = new int[items.size() + 1];
        private final int[] len = new int[items.size()];
        private final int[] ulink;
        private final int[] dlink;
        private final int[] top;
        private final int[] color;

        Solutions() {
            if (N1 == 0) N1 = N;

            for (int i = 1; i < llink.length; ++i) {
                rlink[i-1] = i;
                llink[i] = i-1;
            }
            llink[N+1] = N;
            rlink[N] = N+1;
            llink[N1+1] = N+1;
            rlink[N+1] = N1+1;
            llink[0] = N1;
            rlink[N1] = 0;

            int nxnodes = items.size() + options.size() + 1;  // all the headers + all the spacers
            for (List<?> l : options) {
                nxnodes += l.size();
            }
            ulink = new int[nxnodes];
            dlink = new int[nxnodes];
            top = new int[nxnodes];
            color = new int[nxnodes];
            // holds original order in vertical list
            int[] vindex = new int[nxnodes];

            for (int i = 0; i < ulink.length; ++i) {
                ulink[i] = dlink[i] = i;
            }

            int M = 0;
            int p = N+1;
            top[p] = 0;
            int q;

            for (List<Option> os : options) {
                for (int j = 1; j <= os.size(); ++j) {
                    Option o = os.get(j-1);
                    int ij = o.item;
                    ++len[ij];
                    vindex[p+j] = len[ij];

                    q = ulink[ij];
                    ulink[p+j] = q;
                    dlink[q] = p+j;
                    dlink[p+j] = ij;
                    ulink[ij] = p+j;
                    top[p+j] = ij;
                    color[p+j] = o.color;
                }
                final int k = os.size();
                ++M;
                dlink[p] = p+k;
                p += k+1;
                top[p] = -M;
                ulink[p] = p-k;
            }
            // original length of vertical list
            int[] olen = new int[items.size()];
            System.arraycopy(len, 1, olen, 1, len.length - 1);
        }

        private void hide(int p) {
            // System.out.printf("hide %d\n", p);
            int q = p + 1;
            while (q != p) {
                if (color[q] < 0) {
                    ++q;
                    continue;
                }
                int x = top[q];
                int u = ulink[q];
                int d = dlink[q];
                if (x <= 0) {
                    q = u;
                } else {
                    dlink[u] = d;
                    ulink[d] = u;
                    --len[x];
                    ++q;
                }
            }
        }

        private void cover(int i) {
            // System.out.printf("cover %d\n", i);
            int p = dlink[i];
            while (p != i) {
                hide(p);
                p = dlink[p];
            }
            int l = llink[i];
            int r = rlink[i];
            rlink[l] = r;
            llink[r] = l;
        }

        private void uncover(int i) {
            // System.out.printf("uncover %d\n", i);
            int l = llink[i];
            int r = rlink[i];
            rlink[l] = i;
            llink[r] = i;
            int p = ulink[i];
            while (p != i) {
                unhide(p);
                p = ulink[p];
            }
        }

        private void unhide(int p) {  // this is unhide' from (53) of 7.2.2.1
            // System.out.printf("unhide %d\n", p);
            int q = p - 1;
            while (q != p) {
                if (color[q] < 0) {
                    --q;
                    continue;
                }
                int x = top[q];
                int u = ulink[q];
                int d = dlink[q];
                if (x <= 0) {
                    q = d;  // q was a spacer
                } else {
                    dlink[u] = q;
                    ulink[d] = q;
                    ++len[x];
                    --q;
                }
            }
        }

        private void commit(int p, int j) {  // 7.2.2.1 (54)
            final int c = color[p];
            if (c == 0) cover(j);
            else if (c > 0) purify(p);
        }

        private void purify(int p) {  // 7.2.2.1 (55)
            final int c = color[p];
            final int i = top[p];
            for (int q = dlink[i]; q != i; q = dlink[q]) {
                if (color[q] != c) hide(q);
                else if (q != p) color[q] = -1;
            }
        }

        private void uncommit(int p, int j)  {  // 7.2.2.1 (56)
            int c = color[p];
            if (c == 0) uncover(j);
            else if (c > 0) unpurify(p);
        }

        private void unpurify(int p) {  // 7.2.2.1 (57)
            final int c = color[p];
            final int i = top[p];
            for (int q = ulink[i]; q != i; q = ulink[q]) {
                if (color[q] < 0) color[q] = c;
                else if (q != p) unhide(q);
            }
        }

        private int step = 2;
        private int i = 0;
        private int l = 0;
        private final int[] x = new int[options.size()];
        private long stepCount = 0;
        private long solCount = 0;
        private long logCheckSteps = 10000;

        /**
         * Solve the exact cover problem. Announces each solution via supplied consumer and returns true.
         * Returns false when all solutions (zero or more) have been found.
         */
        @Override
        public boolean tryAdvance(Consumer<? super List<Integer>> action) {
            // print();
            while (true) {
                ++stepCount;
                switch (step) {
                    // The cases are numbered in accordance with Algorithm D's steps. The switch
                    // is used to accomplish the go-tos between steps.
                    case 2:  // Enter level l.
                        if (rlink[0] == 0) {
                            // visit
                            ArrayList<Integer> solution = Lists.newArrayListWithCapacity(l);
                            for (int k = 0; k < l; ++k) {
                                // Any x_i might be in the middle of an option; walk backward
                                // to find the start of the option.
                                int xk = x[k];
                                while(top[xk] > 0) --xk;
                                solution.add(-top[xk]);
                            }
                            step = 8;
                            ++solCount;
                            action.accept(solution);
                            return true;
                        }
                        // case 3:  // Choose i.
                        switch (strategy) {
                            case FIRST:
                                i = rlink[0];
                                break;
                            case MRV:
                                int minLen = Integer.MAX_VALUE;
                                for (int ic = rlink[0]; ic != 0; ic = rlink[ic]) {
                                    int l = len[ic];
                                    if (l < minLen) {
                                        minLen = l;
                                        i = ic;
                                    }
                                }
                                break;
                        }
                        // case 4:  // Cover i.
                        cover(i);
                        x[l] = dlink[i];
                    case 5:  // Try x[l].
                        if (x[l] == i) {
                            step = 7;
                            continue;
                        }
                        for (int p = x[l] + 1; p != x[l]; ) {
                            int j = top[p];
                            if (j <= 0) {
                                p = ulink[p];
                            } else {
                                commit(p, j);
                                ++p;
                            }
                        }
                        ++l;
                        step = 2;
                        continue;
                    case 6:  // Try again.
                        for (int p = x[l] - 1; p != x[l]; ) {
                            int j = top[p];
                            if (j <= 0) {
                                p = dlink[p];
                            } else {
                                uncommit(p, j);
                                --p;
                            }
                        }
                        i = top[x[l]];
                        x[l] = dlink[x[l]];
                        step = 5;
                        continue;
                    case 7:  // Backtrack.
                        uncover(i);
                    case 8:  // Leave level l.
                        if (l == 0) {
                            return false;
                        }
                        --l;
                        step = 6;
                }
            }
        }


        @Override
        public Spliterator<List<Integer>> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return 0;
        }
    }

    public static ExactCoverProblem parseFrom(String problemDescription) {
        return parseFrom(new StringReader(problemDescription));
    }

    /**
     * Produces a StreamTokenizer adapted to the input language for XC problems.
     * @param r the input which the returned tokenizer will consume
     * @return the new StreamTokenizer instance
     */
    private static StreamTokenizer tokenizer(Reader r) {
        StreamTokenizer t = new StreamTokenizer(r);
        t.resetSyntax();
        t.wordChars('a', 'z');
        t.wordChars('A', 'Z');
        t.wordChars('0', '9');
        t.wordChars('-', '-');
        t.wordChars('+', '+');
        t.wordChars(',', ',');
        t.whitespaceChars(0, ' ');
        t.ordinaryChar(';');
        t.eolIsSignificant(true);
        return t;
    }

    /**
     * Parses a complete problem description. The format accepted is that described by Knuth
     * (first line: item names separated by whitespace; each subsequent
     * line is one option, containing a subset of items). The first line
     * may contain one semicolon which separates primary from secondary
     * options.
     * @param problemDescription textual description of XC problem
     * @return a problem instance from which solutions may be generated
     */
    public static ExactCoverProblem parseFrom(Reader problemDescription) {
        StreamTokenizer tz = tokenizer(problemDescription);
        ExactCoverProblem p = new ExactCoverProblem();
        int token;
        try {
            while ((token = tz.nextToken()) != StreamTokenizer.TT_EOL) {
                switch (token) {
                    case ';':
                        p.addItem(";");
                        break;
                    case StreamTokenizer.TT_WORD:
                        p.addItem(tz.sval);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected token " + tz);
                }
            }
            // Now that we're reading options, allow : within a word
            tz.wordChars(':', ':');
            List<String> option = new ArrayList<>();
            while (true) {
                option.clear();
                while ((token = tz.nextToken()) == StreamTokenizer.TT_WORD) {
                    option.add(tz.sval);
                }
                if (token == StreamTokenizer.TT_EOF) {
                    p.addOption(option);
                    break;
                }
                if (token == StreamTokenizer.TT_EOL) {
                    p.addOption(option);
                    continue;
                }
                throw new IllegalArgumentException("Bad token: " + tz);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Parse error", e);
        }
        return p;
    }
}
