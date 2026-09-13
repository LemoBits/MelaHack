package thunder.hack.utility.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public final class GuiMatrix {
    private GuiMatrix() {
    }

    public static Matrix4f positionMatrix(Object matrices) {
        if (matrices instanceof PoseStack stack) {
            return stack.last().pose();
        }
        if (matrices instanceof Matrix3x2fStack stack) {
            return new Matrix4f().mul(stack);
        }
        throw new IllegalArgumentException("Unsupported matrix stack: " + matrices.getClass().getName());
    }

    public static PoseStack asMatrixStack(Object matrices) {
        if (matrices instanceof PoseStack stack) {
            return stack;
        }

        PoseStack stack = new PoseStack();
        stack.last().pose().set(positionMatrix(matrices));
        return stack;
    }

    public static void push(Object matrices) {
        if (matrices instanceof PoseStack stack) {
            stack.pushPose();
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.pushMatrix();
        }
    }

    public static void pop(Object matrices) {
        if (matrices instanceof PoseStack stack) {
            stack.popPose();
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.popMatrix();
        }
    }

    public static void translate(Object matrices, double x, double y, double z) {
        if (matrices instanceof PoseStack stack) {
            stack.translate((float) x, (float) y, (float) z);
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.translate((float) x, (float) y);
        }
    }

    public static void scale(Object matrices, float x, float y, float z) {
        if (matrices instanceof PoseStack stack) {
            stack.scale(x, y, z);
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.scale(x, y);
        }
    }

    public static void rotateZDegrees(Object matrices, float degrees) {
        if (matrices instanceof PoseStack stack) {
            stack.mulPose(Axis.ZP.rotationDegrees(degrees));
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.rotate((float) Math.toRadians(degrees));
        }
    }

    public static void rotateZRadians(Object matrices, float radians) {
        if (matrices instanceof PoseStack stack) {
            stack.mulPose(Axis.ZP.rotation(radians));
        } else if (matrices instanceof Matrix3x2fStack stack) {
            stack.rotate(radians);
        }
    }
}
