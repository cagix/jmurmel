package io.github.jmurmel;

import java.io.StringReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BackquoteTest {

    @Test
    public void testNumber() {
        assertExpansion("1", "1");
    }

    @Test
    public void testSymbol() {
        assertExpansion("aaa", "aaa");
    }

    @Test
    public void testList() {
        assertExpansion("(aaa bbb ccc)", "(aaa bbb ccc)");
    }



    @Test
    public void testQuotedNumber() {
        assertExpansion("'1", "(quote 1)");
    }

    @Test
    public void testQuotedSymbol() {
        assertExpansion("'aaa", "(quote aaa)");
    }

    @Test
    public void testQuotedList() {
        assertExpansion("'(aaa bbb ccc)", "(quote (aaa bbb ccc))");
    }

    @Test
    public void testListQuotedAtoms() {
        assertExpansion("('aaa bbb 'ccc)", "((quote aaa) bbb (quote ccc))");
    }

    @Test
    public void testListQuotedList() {
        assertExpansion("('aaa bbb '(ccc ddd))", "((quote aaa) bbb (quote (ccc ddd)))");
    }

    @Test
    public void testQuotedListQuotedList() {
        assertExpansion("'('aaa bbb '(ccc ddd))", "(quote ((quote aaa) bbb (quote (ccc ddd))))");
    }



    @Test
    public void testBQuotedSymbol() {
        eval("`aaa", "aaa");
        assertExpansion("`aaa", "(quote aaa)");
    }

    @Test
    public void testBQuotedQuotedSymbol() {
        eval("`'aaa", "(quote aaa)");
        assertExpansion("`'aaa", "(quote (quote aaa))");
    }

    @Test
    public void testBQuotedUnquotedSymbol() {
        eval("(define aaa 'aval) `,aaa", "aval");
        assertExpansion("`,aaa", "aaa");
    }


    @Test
    public void testUnquotedNil() {
        eval("`(a ,nil)", "(a nil)");
        assertExpansion("`(a ,nil)", "(list (quote a) nil)");
    }

    @Test
    public void testUnquoteSplicedNil() {
        eval("`(a ,@nil)", "(a)");
        assertExpansion("`(a ,@nil)", "(quote (a))");
    }


    @Test
    public void testBQinBQ() {
        eval("(define c \"C\") (define d \"D\") `(a b ,(if t `,c `,d))", "(a b \"C\")");
    }

    @Test
    public void testBQuotedList() {
        eval("`(aaa bbb ccc)", "(aaa bbb ccc)");
        assertExpansion("`(aaa bbb ccc)", "(quote (aaa bbb ccc))");
    }

    @Test
    public void testBQuotedList2() {
        eval("(let ((a 11.0)) `(1.0 2.0 3.0 ,a))", "(1.0 2.0 3.0 11.0)");
        assertExpansion("(let ((a 11.0)) `(1.0 2.0 3.0 ,a))", "(let ((a 11.0)) (list 1.0 2.0 3.0 a))");
    }

    @Test
    public void testBQuotedDottedList() {
        eval("`(aaa bbb . ccc)", "(aaa bbb . ccc)");
        assertExpansion("`(aaa bbb . ccc)", "(quote (aaa bbb . ccc))");
    }

    @Test
    public void testBQuotedListSplicedList() {
        eval("(define l '(1.0 2.0)) `(a ,@l b)", "(a 1.0 2.0 b)");
        assertExpansion("`(a ,@l b)", "(cons (quote a) (append l (quote (b))))");
    }

    @Test
    public void testBQuotedListSplicedList2() {
        eval("(define l '(1.0 2.0)) `(a ,@l)", "(a 1.0 2.0)");
        assertExpansion("`(a ,@l)", "(cons (quote a) l)");
    }

    @Test
    public void testBQuotedListSplicedFunc() {
        eval("(defun l () '(1.0 2.0)) `(a ,@(l))", "(a 1.0 2.0)");
        assertExpansion("`(a ,@(l))", "(cons (quote a) (l))");
    }

    @Test
    public void testBQuotedListSplicedFunc2() {
        eval("(defun l (p1 p2) '(1.0 2.0)) `(a ,@(l 1 2))", "(a 1.0 2.0)");
        assertExpansion("`(a ,@(l p1 p2))", "(cons (quote a) (l p1 p2))");
    }

    // sample from CLHS
    // http://www.lispworks.com/documentation/HyperSpec/Body/02_df.htm
    // Murmel: (define       a "A") (define       c "C") (define       d '("D" "DD"))   `((,a b) ,c ,@d)  ==> (("A" b) "C" "D" "DD")
    // CL:     (defparameter a "A") (defparameter c "C") (defparameter d '("D" "DD"))   `((,a b) ,c ,@d)  ==> (("A" B) "C" "D" "DD")
    @Test
    public void testCHLSBackQuote() {
        eval("(define a \"A\") (define c \"C\") (define d '(\"D\" \"DD\")) `((,a b) ,c ,@d)", "((\"A\" b) \"C\" \"D\" \"DD\")");
        assertExpansion("`((,a b) ,c ,@d)", "(list* (cons a (quote (b))) c d)");
    }

    @Test
    public void testCHLSMod() {
        eval("(define a \"A\") (define c \"C\") (define d '(\"D\" \"DD\")) `((,a b) ,@d ,c)", "((\"A\" b) \"D\" \"DD\" \"C\")");
        assertExpansion("`((,a b) ,@d ,c)", "(cons (cons a (quote (b))) (append d (cons c nil)))");
    }

    // sample from r7rs.pdf p. 21
    @Test
    public void testR7rs() {
        eval("(let ((a 3)) `((1 2) ,a ,4 ,'five 6))", "((1.0 2.0) 3.0 4.0 five 6.0)");
        assertExpansion("`((1 2) ,a ,4 ,'five 6))", "(list* (quote (1 2)) a 4 (quote five) (quote (6)))");
    }

    // sample from Ansi Common Lisp pp413
    @Test
    public void testACL() {
        eval("(define  x  'a) "
           + "(define  a  1) "
           + "(define  y  'b) "
           + "(define  b  2.0) "
           + "(eval ``(w ,x ,,y))", "(w a 2.0)");
    }

    @Test
    public void testBBquotedSymbol() {
        eval("``a", "(quote a)");
        assertExpansion("``a", "(quote (quote a))");
    }

    @Test
    public void testNested1() {
        eval("(let ((q '(r s))) ``(foo ,@,@q))", "(cons (quote foo) (append r s))");
        assertExpansion("(let ((q '(r s))) ``(foo ,@,@q))", "(let ((q (quote (r s)))) (list (quote cons) (quote (quote foo)) (cons (quote append) q)))");
    }

    @Test
    public void testNested2() {
        eval("(let ((q '(r s))) ``(foo . ,,@q))", "(append (quote (foo)) r s)");
        assertExpansion("(let ((q '(r s))) ``(foo . ,,@q))", "(let ((q (quote (r s)))) (list* (quote append) (list (quote quote) (quote (foo))) q))");
    }

    @Test
    public void testNested3() {
        eval("(let ((q '(r s))) ``(foo ,,@q))", "(list (quote foo) r s)");
        assertExpansion("(let ((q '(r s))) ``(foo ,,@q))", "(let ((q (quote (r s)))) (list* (quote list) (quote (quote foo)) q))");
    }

    @Test
    public void testNested4() {
        eval("(let ((q '(r s))) ``(,@,@q))", "(append r s)");
        assertExpansion("(let ((q '(r s))) ``(,@,@q))", "(let ((q (quote (r s)))) (cons (quote append) q))");
    }

    @Test
    public void testNested5() {
        eval("(let ((q '(r s))) ``(,@,@q ,@,@q))", "(append r s r s)");
        assertExpansion("(let ((q '(r s))) ``(,@,@q ,@,@q))", "(let ((q (quote (r s)))) (cons (quote append) (append q q)))");
    }

    @Test
    public void testNested6() {
        eval("(let ((q '(r s))) ``(,@,@q ,@,@q ,@,@q))", "(append r s r s r s)");
        assertExpansion("(let ((q '(r s))) ``(,@,@q ,@,@q ,@,@q))", "(let ((q (quote (r s)))) (cons (quote append) (append q q q)))");
    }

    // ``(aaa ,bbb ,,ccc) =>
    @Test
    public void testX() {
        // without optimization
        // eval("(define ccc 'cccval) ``(aaa ,bbb ,,ccc)", "(append (quote (aaa)) (append (list bbb) (list cccval)))");
        // assertExpansion("``(aaa ,bbb ,,ccc)", "(append (quote (append)) "
        //                                             + "(append (list (append (quote (quote)) (list (quote (aaa))))) "
        //                                                     + "(list (append (quote (append)) "
        //                                                                   + "(append (list (append (quote (list)) (quote (bbb)))) "
        //                                                                           + "(list (append (quote (list)) (list ccc))))))))");

        // with optimization
        eval("(define ccc 'cccval) ``(aaa ,bbb ,,ccc)", "(list (quote aaa) bbb cccval)");
        assertExpansion("``(aaa ,bbb ,,ccc)", "(list (quote list) (quote (quote aaa)) (quote bbb) ccc)");
    }

    @Test
    public void testSpliceList() {
        eval("(define a 'aval) (define b 'bval) (define y 'b) (define l '(a b)) (eval ``(,a ,,@l ,,y))", "(aval aval bval bval)");
    }

    @Test
    public void testMultiSplice() {
        assertExpansion("`(,@a ,@b ,@c ,@d)", "(append a b c d)");
    }

    @Test
    public void testMultiSplice2() {
        assertExpansion("``(,,@a ,,@b ,,@c ,,@d)", "(cons (quote list) (append a b c d))");
    }

    @Test
    public void testNil() {
        eval("`(nil)", "(nil)");
    }

    @Test
    public void testNil2() {
        eval("`(setq a nil)", "(setq a nil)");
    }

    @Test
    public void testNil3() {
        eval("`(a nil b)", "(a nil b)");
    }

    @Test
    public void testNil4() {
        eval("`(if a nil 'a-is-nil)", "(if a nil (quote a-is-nil))");
    }


    @Test
    public void errortestUnquote() {
        assertError("(,b)", "comma is not inside a backquote");
    }

    @Test
    public void errortestUnquoteSplice() {
        assertError("`,@b", "can't splice here");
    }



    private static void assertExpansion(String expression, String expectedExpansion) {
        final Object expanded = expand(expression);
        final String expandedSexp = TestUtils.sexp(expanded);
        assertEquals(expectedExpansion, expandedSexp);
    }

    private static void assertError(String s, String expectedError) {
        try {
            expand(s);
            fail("expected error " + expectedError);
        }
        catch (Exception e) {
            assertTrue("unexpected exception " + e.getClass().getSimpleName() + ": " + e.getMessage(), e instanceof LambdaJ.LambdaJError);
            assertTrue("expected <" + expectedError + "> but got <" + e.getMessage() + '>', e.getMessage().startsWith(expectedError));
        }
    }

    private static Object expand(String s) {
        final LambdaJ.ObjectReader reader = LambdaJ.makeReader(new StringReader(s)::read, new LambdaJ.ListSymbolTable(), null);
        return reader.readObj(null);
    }

    private static void eval(String exp, String expectedResult) {
        LambdaJTest.runTest("backquotetest.lisp", exp, expectedResult, null);
    }
}
