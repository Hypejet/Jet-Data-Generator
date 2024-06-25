package net.hypejet.jet.data.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import net.hypejet.jet.data.generator.util.CodeBlocks;
import net.hypejet.jet.data.generator.util.JavaDocBuilder;
import net.hypejet.jet.data.generator.util.JavaFileUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a {@linkplain Generator generator}, which generates data as constants, each with the
 * same {@linkplain Type type}.
 *
 * @since 1.0
 * @author Codestech
 * @see Generator
 */
public abstract class ConstantGenerator implements Generator {

    private final String outputPackage;

    private final ClassName outputClassName;
    private final CodeBlock outputClassJavaDoc;

    private final Type constantFieldType;

    private final CodeBlock valuesMethodJavaDoc;

    /**
     * Constructs the {@linkplain ConstantGenerator constant generator}.
     *
     * @param outputPackage a package of the output java file
     * @param outputClassName a name of the output java class
     * @param constantFieldType a type of the constant fields
     * @param outputClassJavaDoc a javadoc of the output java class
     * @param valuesMethodJavaDoc a javadoc of a method returning all values generated using this generator
     * @since 1.0
     */
    public ConstantGenerator(@NonNull String outputPackage, @NonNull ClassName outputClassName,
                             @NonNull Type constantFieldType, @NonNull CodeBlock outputClassJavaDoc,
                             @NonNull CodeBlock valuesMethodJavaDoc) {
        this.outputPackage = outputPackage;
        this.outputClassName = outputClassName;
        this.constantFieldType = constantFieldType;
        this.outputClassJavaDoc = outputClassJavaDoc;
        this.valuesMethodJavaDoc = valuesMethodJavaDoc;
    }

    @Override
    public final @NonNull JavaFile generate(@NonNull Logger logger) {
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(this.outputClassName)
                .addJavadoc(this.outputClassJavaDoc)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        Collection<FieldSpec> entityTypeFieldSpecs = this.generateFields(logger);
        List<CodeBlock> fieldReferencingCodeBlocks = new ArrayList<>();

        for (FieldSpec fieldSpec : entityTypeFieldSpecs) {
            specBuilder.addField(fieldSpec);
            fieldReferencingCodeBlocks.add(CodeBlock.of(fieldSpec.name));
        }

        ParameterizedTypeName valueCollectionTypeName = ParameterizedTypeName
                .get(Collection.class, this.constantFieldType)
                .annotated(Collections.singletonList(AnnotationSpec.builder(NonNull.class).build()));

        FieldSpec valuesFieldSpec = FieldSpec.builder(valueCollectionTypeName, "VALUES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(CodeBlocks.setCreator(fieldReferencingCodeBlocks.toArray()))
                .build();
        specBuilder.addField(valuesFieldSpec);

        specBuilder.addMethod(MethodSpec.methodBuilder("values")
                .returns(valueCollectionTypeName)
                .addStatement(CodeBlocks.returning(valuesFieldSpec.name))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc(this.valuesMethodJavaDoc)
                .build());

        return JavaFile.builder(this.outputPackage, specBuilder.build())
                .indent(JavaFileUtil.INDENT)
                .build();
    }

    public abstract @NonNull Collection<FieldSpec> generateFields(@NonNull Logger logger);
}