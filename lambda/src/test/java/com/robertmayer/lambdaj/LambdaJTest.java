package com.robertmayer.lambdaj;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.robertmayer.lambdaj.LambdaJ;

public class LambdaJTest {

    private String[][] tests = {
        /*  0 */ { "write", "#<primitive>", null },

        // application of builtins
        /*  1 */ { "(write (quote (Hello, world!)))", "(quote t)", "(Hello, world!)" },
        /*  2 */ { "(write (quote HELLO))", "(quote t)", "HELLO" },
        /*  3 */ { "(cons (quote HELLO) (quote HELLO))", "(HELLO . HELLO)", null },
        /*  4 */ { "(assoc (quote write) ())", "nil", null },
        /*  5 */ { "(assoc (quote b) (cons (cons (quote a) (quote 1)) (cons (cons (quote b) (quote 2)) (cons (cons (quote c) (quote 3)) ()))))", "(b . 2)", null },

        // comments
        /*  6 */ { "; comment\n(write (quote HELLO))", "(quote t)", "HELLO" },
        /*  7 */ { "; comment\n(write (quote HELLO)) ; comment", "(quote t)", "HELLO" },

        // quoted chars
        /*  8 */ { "(write (quote HELLO\\ ))", "(quote t)", "HELLO " },
        /*  9 */ { "(write (quote HELLO\\\\))", "(quote t)", "HELLO\\" },
        /* 10 */ { "(write (quote HELLO\\)))", "(quote t)", "HELLO)" },
        /* 11 */ { "(write (quote HELLO\\;))", "(quote t)", "HELLO;" },

        // apply
        /* 12 */ { "(apply write (cons (quote HELLO) nil))", "(quote t)", "HELLO" },
        /* 13 */ { "(apply write (cons (cons (quote HELLO) (cons (quote HELLO) nil)) nil))", "(quote t)", "(HELLO HELLO)" },
        /* 14 */ { "(apply write (cons (cons (quote HELLO) (cons (quote HELLO) ())) ()))", "(quote t)", "(HELLO HELLO)" },
        /* 15 */ { "(apply write (cons (cons (quote HELLO) (quote HELLO)) nil))", "(quote t)", "(HELLO . HELLO)" },

        // lambda
        /* 16 */ { "(lambda () (write (quote noparam)))", "(lambda nil (write (quote noparam)))", null },
        /* 17 */ { "(write (lambda () (write (quote noparam))))", "(quote t)", "(lambda nil (write (quote noparam)))" },
        /* 18 */ { "((lambda () (write (quote noparam))))", "(quote t)", "noparam" },
        /* 19 */ { "((lambda (x) (write x)) (quote hello))", "(quote t)", "hello" },
        /* 20 */ { "((lambda (x y) (write (cons x y))) (quote p1) (quote p2))", "(quote t)", "(p1 . p2)" },
        /* 21 */ { "(write ((lambda () (write (quote 1)) (write (quote 2)))))", "(quote t)", "12(quote t)" },

        // eq
        /* 22 */ { "(write ((lambda () (eq (quote 1) (quote 2)))))", "(quote t)", "nil" },
        /* 23 */ { "(write ((lambda () (eq (quote 1) (quote 1)))))", "(quote t)", "(quote t)" },

        // cond
        /* 24 */ { "((lambda (x) (cond ((eq x (quote 1)) (write (quote 1))) ((eq x (quote 2)) (write (quote 2))) ((eq x (quote 3)) (write (quote 3))))) (quote 3))", "(quote t)", "3" },
        /* 25 */ { "((lambda (x) (cond ((eq x (quote 1)) (write (quote 1))) ((eq x (quote 2)) (write (quote 2))) ((eq x (quote 3)) (write (quote 3))))) (quote 4))", "nil", null },

        // labels
        /* 26 */ { "(labels () (write (quote 1)) (write (quote 2)))", "(quote t)", "12" },
        /* 27 */ { "(labels ((w1 (x) (write (cons (quote 1) x))) (w2 (x) (write (cons (quote 2) x)))) (w1 (quote 3)) (w2 (quote 4)))", "(quote t)", "(1 . 3)(2 . 4)" }, // todo
    };

    //@Test
    public void runTest() {
        runTest(27);
    }

    @Test
    public void allTests() {
        for (int n = 0; n < tests.length; n++) {
            runTest(n);
        }
    }

    private void runTest(int n) {
        String[] test = tests[n];
        String prog = test[0];
        String expectedResult = test[1];
        String expectedOutput = test[2];

        runTest(getClass().getSimpleName() + ".tests[" + n + "]", prog, expectedResult, expectedOutput);
    }



    //@Test
    public void runFile() throws Exception {
        runTest(Paths.get("src", "test", "lisp", "tailrec.lisp"));
    }

    @Test
    public void runAllFiles() throws Exception {
        Path cwd = Paths.get(".").toRealPath();
        System.out.println("cwd: " + cwd.toString());
        Path lispDir = Paths.get("src", "test", "lisp");
        Files.walk(lispDir).filter(path -> path.toString()
                .endsWith(".lisp")).forEach(path -> runTest(path));
    }

    private Pattern outputPattern = Pattern.compile("[^;]*; output: (.*)");
    private Pattern resultPattern = Pattern.compile("[^;]*; result: (.*)");
    private Pattern errorPattern = Pattern.compile("[^;]*; error: (.*)");

    private void runTest(Path fileName) {
        try {
        final String contents = new String(Files.readAllBytes(fileName));

        final String expectedOutput = findMatch(outputPattern, contents);
        final String expectedResult = findMatch(resultPattern, contents);
        final String expectedError = findMatch(errorPattern, contents);

        if (expectedOutput != null && expectedResult != null || expectedError != null) {
            runTest(fileName.toString(), contents, expectedResult, expectedOutput);
        }
        else {
            System.out.println("***** skipping " + fileName.toString());
        }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String findMatch(Pattern pattern, String contents) {
        final Matcher outputMatcher = pattern.matcher(contents);
        if (outputMatcher.find()) {
            return outputMatcher.group(1);
        }
        else {
            return null;
        }
    }



    private void runTest(String fileName, String prog, String expectedResult, String expectedOutput) {
        InputStream in = new ByteArrayInputStream(prog.getBytes());

        ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(actualOutput);

        LambdaJ interpreter = new LambdaJ();
        interpreter.trace = LambdaJ.TRC_PRIM;

        System.out.println("***** running program:");
        System.out.println("-------------------------------------------------------");
        System.out.println(prog);
        System.out.println("-------------------------------------------------------");
        String actualResult = interpreter.interpret(in, out);
        out.flush();
        System.out.println("***** done program, result: " + actualResult);
        System.out.println();

        assertEquals("program " + fileName + " produced unexpected result", expectedResult, actualResult);

        if (expectedOutput == null) {
            assertEquals("program " + fileName + " produced unexpected output", 0, actualOutput.size());
        } else {
            assertEquals("program " + fileName + " produced unexpected output", expectedOutput, actualOutput.toString());
        }
    }
}
