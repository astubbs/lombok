package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.PKG.*;

import lombok.Getter;
import lombok.core.AnnotationValues;
import lombok.core.TransformationsUtil;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseAST.Node;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.mangosdk.spi.ProviderFor;

@ProviderFor(EclipseAnnotationHandler.class)
public class HandleGetter implements EclipseAnnotationHandler<Getter> {
	@Override public boolean handle(AnnotationValues<Getter> annotation, Annotation ast, Node annotationNode) {
		if ( !(annotationNode.up().get() instanceof FieldDeclaration) ) return false;
		FieldDeclaration field = (FieldDeclaration) annotationNode.up().get();
		TypeReference fieldType = field.type;
		String getterName = TransformationsUtil.toGetterName(
				new String(field.name), nameEquals(fieldType.getTypeName(), "boolean"));
		
		TypeDeclaration parent = (TypeDeclaration) annotationNode.up().up().get();
		if ( parent.methods != null ) for ( AbstractMethodDeclaration method : parent.methods ) {
			if ( method.selector != null && new String(method.selector).equals(getterName) ) {
				annotationNode.addWarning(String.format(
						"Not generating %s(): A method with that name already exists",  getterName));
				return false;
			}
		}
		
		int modifier = toModifier(annotation.getInstance().value());
		
		MethodDeclaration method = generateGetter(parent, field, getterName, modifier, ast);
		
		if ( parent.methods == null ) {
			parent.methods = new AbstractMethodDeclaration[1];
			parent.methods[0] = method;
		} else {
			AbstractMethodDeclaration[] newArray = new AbstractMethodDeclaration[parent.methods.length + 1];
			System.arraycopy(parent.methods, 0, newArray, 0, parent.methods.length);
			newArray[parent.methods.length] = method;
			parent.methods = newArray;
		}
		
		return true;
	}
	
	private MethodDeclaration generateGetter(TypeDeclaration parent, FieldDeclaration field, String name,
			int modifier, Annotation ast) {
		MethodDeclaration method = new MethodDeclaration(parent.compilationResult);
		method.modifiers = modifier;
		method.returnType = field.type;
		method.annotations = null;
		method.arguments = null;
		method.selector = name.toCharArray();
		method.binding = null;
		method.thrownExceptions = null;
		method.typeParameters = null;
		method.scope = parent.scope == null ? null : new MethodScope(parent.scope, method, false);
		method.bits |= ASTNode.Bit24;
		Expression fieldExpression = new SingleNameReference(field.name, (field.declarationSourceStart << 32) | field.declarationSourceEnd);
		Statement returnStatement = new ReturnStatement(fieldExpression, field.sourceStart, field.sourceEnd);
		method.bodyStart = method.declarationSourceStart = method.sourceStart = ast.sourceStart;
		method.bodyEnd = method.declarationSourceEnd = method.sourceEnd = ast.sourceEnd;
		method.statements = new Statement[] { returnStatement };
		return method;
	}
}