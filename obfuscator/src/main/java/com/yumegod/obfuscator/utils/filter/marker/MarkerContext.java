package com.yumegod.obfuscator.utils.filter.marker;

import com.yumegod.obfuscation.*;
import com.yumegod.obfuscator.utils.SimpleNoRepeatArrayList;
import com.yumegod.obfuscator.utils.cfg.ConfigManager;
import com.yumegod.obfuscator.utils.filter.ClassFilterExpr;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MarkerContext {
    public static final List<MarkerContext> CONTEXTS = new ArrayList<>();
    public final String path;
    public final List<ClassFilterExpr> expressions = new ArrayList<>(2);
    public Consumer<ClassNode> onClassMatched;
    public Consumer<MethodNode> onMethodMatched;
    public Consumer<FieldNode> onFieldMatched;

    public MarkerContext(String path, Consumer<ClassNode> onClassMatched, Consumer<MethodNode> onMethodMatched, Consumer<FieldNode> onFieldMatched) {
        this.path = path;

        this.onClassMatched = onClassMatched;
        this.onMethodMatched = onMethodMatched;
        this.onFieldMatched = onFieldMatched;

        resolve();
    }

    public void resolve() {
        expressions.clear();
        if (path != null) {
            List<String> list = ConfigManager.getStringList(path);
            if (list != null) for (String s : list) {
                expressions.add(new ClassFilterExpr(s));
            }
        }
    }

    private static void addAnnotation(ClassNode node, String desc) {
        (node.invisibleAnnotations = orDefault(node.invisibleAnnotations)).add(new AnnotationNode(desc));
    }

    private static void addAnnotation(MethodNode node, String desc) {
        (node.invisibleAnnotations = orDefault(node.invisibleAnnotations)).add(new AnnotationNode(desc));
    }

    private static void addAnnotation(FieldNode node, String desc) {
        (node.invisibleAnnotations = orDefault(node.invisibleAnnotations)).add(new AnnotationNode(desc));
    }

    private static List<AnnotationNode> orDefault(List<AnnotationNode> annotations) {
        return !(annotations instanceof SimpleNoRepeatArrayList) ? new SimpleNoRepeatArrayList<>(annotations) : annotations;
    }

    static {
        // native
        {
            String nativeDesc = Type.getDescriptor(Native.class);
            String noNativeDesc = Type.getDescriptor(NotNative.class);
            CONTEXTS.add(new MarkerContext("native.include",
                    node -> addAnnotation(node, nativeDesc),
                    node -> addAnnotation(node, nativeDesc),
                    node -> {}));
            CONTEXTS.add(new MarkerContext("native.exclude",
                    node -> {},
                    node -> addAnnotation(node, noNativeDesc),
                    node -> {}));
        }
        // call encryption
        {
            String callEncryptionDesc = Type.getDescriptor(CallEncryption.class),
                    noCallEncryptionDesc = Type.getDescriptor(NoCallEncryption.class);
            CONTEXTS.add(new MarkerContext("native.call_encryption.include",
                    node -> addAnnotation(node, callEncryptionDesc),
                    node -> addAnnotation(node, callEncryptionDesc),
                    node -> {}));
            CONTEXTS.add(new MarkerContext("native.call_encryption.exclude",
                    node ->addAnnotation(node, noCallEncryptionDesc),
                    node ->addAnnotation(node, noCallEncryptionDesc),
                    node -> {}));
        }
        // rename
        {
            String noRenameDesc = Type.getDescriptor(NoRename.class);
            CONTEXTS.add(new MarkerContext("rename.exclude",
                    node -> addAnnotation(node, noRenameDesc),
                    node -> addAnnotation(node, noRenameDesc),
                    node -> addAnnotation(node, noRenameDesc)));
        }
        // string_obfuscation
        {
            String stringObfuscationDesc = Type.getDescriptor(StringObfuscate.class),
                    noStringObfuscationDesc = Type.getDescriptor(NoStringObfuscate.class);
            CONTEXTS.add(new MarkerContext("string_obfuscation.include",
                    node -> addAnnotation(node, stringObfuscationDesc),
                    node -> {},
                    node -> {}));
            CONTEXTS.add(new MarkerContext("string_obfuscation.exclude",
                    node -> addAnnotation(node, noStringObfuscationDesc),
                    node -> {},
                    node -> {}));
        }
        // flow obfuscation
        {
            String flowObfuscationDesc = Type.getDescriptor(FlowObfuscate.class),
                    noFlowObfuscationDesc = Type.getDescriptor(NoFlowObfuscate.class);
            CONTEXTS.add(new MarkerContext("flow_obfuscation.include",
                    node -> addAnnotation(node, flowObfuscationDesc),
                    node -> {},
                    node -> {}));
            CONTEXTS.add(new MarkerContext("flow_obfuscation.exclude",
                    node -> addAnnotation(node, noFlowObfuscationDesc),
                    node -> {},
                    node -> {}));
        }
        // number obfuscation
        {
            String numberObfuscationDesc = Type.getDescriptor(NumberObfuscate.class),
                    noNumberObfuscationDesc = Type.getDescriptor(NoNumberObfuscate.class);
            CONTEXTS.add(new MarkerContext("number_obfuscation.include",
                    node -> addAnnotation(node, numberObfuscationDesc),
                    node -> {},
                    node -> {}));
            CONTEXTS.add(new MarkerContext("number_obfuscation.exclude",
                    node -> addAnnotation(node, noNumberObfuscationDesc),
                    node -> {},
                    node -> {}));
        }
        // invoke dynamic
        {
            String invokeDynamicDesc = Type.getDescriptor(InvokeDynamic.class);
            CONTEXTS.add(new MarkerContext("invoke_dynamic.include",
                    node -> addAnnotation(node, invokeDynamicDesc),
                    node -> addAnnotation(node, invokeDynamicDesc),
                    node -> {}));
        }
    }
}
