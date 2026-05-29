package thunder.hack.utility.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public final class GuiMatrix {
    private GuiMatrix() {
    }

    public static Matrix4f positionMatrix(Object matrices) {
        if (matrices instanceof MatrixStack stack) {
            return stack.peek().getPositionMatrix();
        }
        if (matrices instanceof Matrix3x2fStack stack) {
            return new Matrix4f().mul(stack);
        }
        throw new IllegalArgumentException("Unsupported matrix stack: " + matrices.getClass().getName());
    }

    public static MatrixStack asMatrixStack(Object matrices) {
        if (matrices instanceof MatrixStack stack) {
            return stack;
        }

        MatrixStack stack = new MatrixStack();
        stack.peek().getPositionMatrix().set(positionMatrix(matrices));
        return stack;
    }

    public static void push(Object matrices) {
        if (matrices instanceof MatrixStack stack) {
            stack.push();
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.pushMatrix();
        }
    }

    public static void pop(Object matrices) {
        if (matrices instanceof MatrixStack stack) {
            stack.pop();
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.popMatrix();
        }
    }

    public static void translate(Object matrices, double x, double y, double z) {
        if (matrices instanceof MatrixStack stack) {
            stack.translate((float) x, (float) y, (float) z);
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.translate((float) x, (float) y);
        }
    }

    public static void scale(Object matrices, float x, float y, float z) {
        if (matrices instanceof MatrixStack stack) {
            stack.scale(x, y, z);
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.scale(x, y);
        }
    }

    public static void rotateZDegrees(Object matrices, float degrees) {
        if (matrices instanceof MatrixStack stack) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.rotate((float) Math.toRadians(degrees));
        }
    }

    public static void rotateZRadians(Object matrices, float radians) {
        if (matrices instanceof MatrixStack stack) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotation(radians));
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.rotate(radians);
        }
    }
}
