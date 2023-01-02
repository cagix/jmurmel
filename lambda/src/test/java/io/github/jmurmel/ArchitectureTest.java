package io.github.jmurmel;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.testng.annotations.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Murmel's "architecture" is kinda messy, try checking/ enforcing some rules.
 * See https://www.archunit.org/userguide/html/000_Index.html
 */
public class ArchitectureTest {
    private static final JavaClasses murmelClasses = new ClassFileImporter()
                                                     .withImportOption(new ImportOption.DoNotIncludeTests())
                                                     .importPackages("io.github.jmurmel");

    @Test
    public void checkMurmel() {

        checkUsers(LambdaJ.Cli.class, LambdaJ.class);

        checkUsers(JavaCompilerHelper.class, LambdaJ.MurmelJavaCompiler.class);
        checkUsers(JavaSourceFromString.class, JavaCompilerHelper.class);
        checkUsers(MurmelClassLoader.class, JavaCompilerHelper.class);
    }

    /** Utility classes whose methods are leafs in the call hierarchy, i.e. don't use any other Murmel classes (other than *Error) */
    @Test
    public void checkLeafClasses() {
        // toplevel classes
        checkLeaf("EolUtil");
        //checkLeaf("JavaCompilerHelper"); // uses JavaSourceFromString, MurmelClassLoader
        checkLeaf("JavaSourceFromString");
        // LambdaJ uses various
        checkLeaf("MurmelClassLoader");
        checkLeaf("TurtleFrame");
        //checkLeaf("UnixToAnyEol"); uses LambdaJ.WriteConsumer.print()
        checkLeaf("WrappingWriter");
    }

    /** check that {@code clazz} is only used by {@code allowedUsers} */
    private static void checkUsers(Class<?> clazz, Class<?>... allowedUsers) {
        theClass(clazz).should().onlyHaveDependentClassesThat().belongToAnyOf(allowedUsers).check(murmelClasses);
    }

    /** check that the class {@code className} only uses itself, nested classes and *Error classes */
    private static void checkLeaf(String className) {
        final DescribedPredicate<? super JavaClass> forbidden = DescribedPredicate.and(HasName.Predicates.nameStartingWith("io"),
                                                                                       DescribedPredicate.not(HasName.Predicates.nameContaining(className)),
                                                                                       DescribedPredicate.not(HasName.Predicates.nameEndingWith("Error")));
        noClasses().that().haveSimpleName(className).should()
                   .accessClassesThat(forbidden)
                   .check(murmelClasses);
    }
}