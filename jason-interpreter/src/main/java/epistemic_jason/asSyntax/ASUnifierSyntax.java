package epistemic_jason.asSyntax;

import epistemic_jason.asSemantics.RewriteUnifier;
import jason.asSyntax.LogicalFormula;
import jason.util.Pair;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ASUnifierSyntax {

    public static final List<List<Pair<LogicalFormula, RewriteUnifier>>> EMPTY_UNIF_MAPPING_LIST = Collections.emptyList();

    public static final List<RewriteUnifier> EMPTY_REWRITE_UNIF_LIST = Collections.emptyList();


    static public Iterator<RewriteUnifier> createRewriteUnifIterator(final RewriteUnifier... unifs) {
        return new Iterator<RewriteUnifier>() {
            int i = 0;

            public boolean hasNext() {
                return i < unifs.length;
            }

            public RewriteUnifier next() {
                return unifs[i++];
            }

            public void remove() {
            }
        };
    }


    static public Iterator<List<Pair<LogicalFormula, RewriteUnifier>>> createRewriteUnifListPairIterator(final Pair<LogicalFormula,RewriteUnifier>... unifs) {
        return new Iterator<List<Pair<LogicalFormula, RewriteUnifier>>>() {
            int i = 0;

            public boolean hasNext() {
                return i < unifs.length;
            }

            public List<Pair<LogicalFormula, RewriteUnifier>> next() {
                return Collections.singletonList(unifs[i++]);
            }

            public void remove() {
            }
        };
    }
}
