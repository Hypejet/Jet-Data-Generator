package net.hypejet.jet.data.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import net.hypejet.jet.data.block.VanillaBlock;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public final class DataGenerator {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        ClassName className = ClassName.get("net.hypejet.jet.data.block", "VanillaBlocks");
        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className)
                .addJavadoc("""
                        Represents a holder of built-in blocks.
                        <p>Code autogenerated, do not edit!</p>
                        
                        @since 1.0
                        @see VanillaBlock""")
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);


        DefaultedRegistry<Block> registry = BuiltInRegistries.BLOCK;
        ClassName blockImplClassName = ClassName.get("net.hypejet.jet.data.block", "VanillaBlockImpl");

        List<FieldSpec> blockFieldSpecs = new ArrayList<>();

        for (Block block : registry) {
            ResourceLocation location = registry.wrapAsHolder(block)
                    .unwrapKey()
                    .map(ResourceKey::location)
                    .orElseThrow(() -> new IllegalArgumentException("The block was not registered"));
            blockFieldSpecs.add(FieldSpec.builder(VanillaBlock.class, location.getPath().toUpperCase())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T($T.key($S, $S))", blockImplClassName, Key.class, location.getNamespace(),
                            location.getPath())
                    .build());
        }

        StringJoiner joiner = new StringJoiner(", ");

        blockFieldSpecs.forEach(spec -> {
            specBuilder.addField(spec);
            joiner.add(spec.name);
        });

        ParameterizedTypeName typeName = ParameterizedTypeName.get(Collection.class, VanillaBlock.class)
                .annotated(Collections.singletonList(AnnotationSpec.builder(NonNull.class).build()));

        FieldSpec valuesFieldSpec = FieldSpec.builder(typeName, "VALUES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.of($L)", Set.class, joiner.toString())
                .build();
        specBuilder.addField(valuesFieldSpec);

        specBuilder.addMethod(MethodSpec.methodBuilder("values")
                .returns(typeName)
                .addStatement("return $L", valuesFieldSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc(CodeBlock.of("""
                        Gets all built-in blocks.
                        
                        @return the blocks
                        @since 1.0
                        @see VanillaBlock
                        """))
                .build());

        TypeSpec spec = specBuilder.build();
        JavaFile file = JavaFile.builder(className.packageName(), spec)
                .indent("    ") // 4 spaces
                .build();

        try {
            file.writeTo(Path.of(args[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}