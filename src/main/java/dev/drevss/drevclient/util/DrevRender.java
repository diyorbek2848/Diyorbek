package dev.drevss.drevclient.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * World-space drawing (boxes, lines, tracers) that ignores the depth buffer, plus
 * world-to-screen projection used by Nametags and the 2D ESP.
 */
public final class DrevRender {
    private static final Minecraft mc = Minecraft.getInstance();

    // ---- pipelines (built on vanilla snippets, own fog-less line shader) ----
    private static final RenderPipeline.Snippet LINES_SNIPPET = RenderPipeline
        .builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
        .withVertexShader(Identifier.parse("drevclient:core/esp_lines"))
        .withFragmentShader(Identifier.parse("drevclient:core/esp_lines"))
        .withBlend(BlendFunction.TRANSLUCENT).withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
        .buildSnippet();

    private static final RenderPipeline ESP_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(LINES_SNIPPET)
            .withLocation(Identifier.parse("drevclient:pipeline/esp_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());

    private static final RenderPipeline DEPTH_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(LINES_SNIPPET)
            .withLocation(Identifier.parse("drevclient:pipeline/depth_lines")).build());

    private static final RenderPipeline ESP_QUADS_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.parse("drevclient:pipeline/esp_quads"))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());

    public static final RenderType ESP_LINES = RenderType.create("drevclient:esp_lines",
        RenderSetup.builder(ESP_LINES_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup());

    public static final RenderType DEPTH_LINES = RenderType.create("drevclient:depth_lines",
        RenderSetup.builder(DEPTH_LINES_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup());

    public static final RenderType ESP_QUADS = RenderType.create("drevclient:esp_quads",
        RenderSetup.builder(ESP_QUADS_PIPELINE).sortOnUpload().createRenderSetup());

    public static float lineWidth = 2f;

    // ---- matrices captured every frame by LevelRendererMixin ----
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJ = new Matrix4f();
    private static boolean haveMatrices;

    private DrevRender() { }

    /** Forces the static pipelines to register during client start-up. */
    public static void init() { }

    public static void captureMatrices(Matrix4f view, Matrix4f projection) {
        VIEW.set(view);
        PROJ.set(projection);
        haveMatrices = true;
    }

    public static Vec3 cameraPos() {
        Camera camera = mc.gameRenderer.getMainCamera();
        return camera == null ? Vec3.ZERO : camera.position();
    }

    // ------------------------------------------------------------------
    // Batched drawing
    // ------------------------------------------------------------------

    public record ColoredBox(AABB box, int fillColor, int lineColor) { }
    public record ColoredLine(Vec3 from, Vec3 to, int color) { }

    public static void drawBoxes(PoseStack matrices, List<ColoredBox> boxes, boolean fill, boolean outline) {
        if (boxes.isEmpty()) return;
        MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        Vec3 cam = cameraPos();
        PoseStack.Pose pose = matrices.last();

        if (fill) {
            VertexConsumer buf = source.getBuffer(ESP_QUADS);
            for (ColoredBox b : boxes) {
                if (((b.fillColor() >>> 24) & 0xFF) == 0) continue;
                quadBox(pose, buf, b.box().move(-cam.x, -cam.y, -cam.z), b.fillColor());
            }
            source.endBatch(ESP_QUADS);
        }
        if (outline) {
            VertexConsumer buf = source.getBuffer(ESP_LINES);
            for (ColoredBox b : boxes) {
                lineBox(pose, buf, b.box().move(-cam.x, -cam.y, -cam.z), b.lineColor());
            }
            source.endBatch(ESP_LINES);
        }
    }

    public static void drawBox(PoseStack matrices, AABB box, int fillColor, int lineColor) {
        drawBoxes(matrices, List.of(new ColoredBox(box, fillColor, lineColor)), true, true);
    }

    /** Lines from the crosshair to each target point. */
    public static void drawTracers(PoseStack matrices, List<ColoredLine> targets) {
        if (targets.isEmpty()) return;
        MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        VertexConsumer buf = source.getBuffer(ESP_LINES);
        Vec3 cam = cameraPos();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 start = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(10);
        PoseStack.Pose pose = matrices.last();
        for (ColoredLine t : targets) {
            Vec3 end = t.to().subtract(cam);
            line(pose, buf, (float) start.x, (float) start.y, (float) start.z,
                (float) end.x, (float) end.y, (float) end.z, t.color());
        }
        source.endBatch(ESP_LINES);
    }

    /** Polyline through world points, e.g. a projectile trajectory. */
    public static void drawPath(PoseStack matrices, List<Vec3> points, int color, boolean throughWalls) {
        if (points.size() < 2) return;
        RenderType type = throughWalls ? ESP_LINES : DEPTH_LINES;
        MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        VertexConsumer buf = source.getBuffer(type);
        Vec3 cam = cameraPos();
        PoseStack.Pose pose = matrices.last();
        for (int i = 1; i < points.size(); i++) {
            Vec3 a = points.get(i - 1).subtract(cam);
            Vec3 b = points.get(i).subtract(cam);
            line(pose, buf, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, color);
        }
        source.endBatch(type);
    }

    // ------------------------------------------------------------------
    // Raw geometry (positions already relative to the camera)
    // ------------------------------------------------------------------

    public static void line(PoseStack.Pose pose, VertexConsumer buf, float x1, float y1, float z1,
                            float x2, float y2, float z2, int color) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (normal.lengthSquared() < 1e-12f) return;
        normal.normalize();
        buf.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, normal).setLineWidth(lineWidth);

        // Lines that pass behind the camera break the line shader; split them at the camera plane.
        float t = new Vector3f(x1, y1, z1).negate().dot(normal);
        float length = new Vector3f(x2 - x1, y2 - y1, z2 - z1).length();
        if (t > 0 && t < length) {
            Vector3f mid = new Vector3f(normal).mul(t).add(x1, y1, z1);
            buf.addVertex(pose, mid).setColor(color).setNormal(pose, normal).setLineWidth(lineWidth);
            buf.addVertex(pose, mid).setColor(color).setNormal(pose, normal).setLineWidth(lineWidth);
        }
        buf.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, normal).setLineWidth(lineWidth);
    }

    public static void lineBox(PoseStack.Pose pose, VertexConsumer buf, AABB b, int c) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        // bottom
        line(pose, buf, x1, y1, z1, x2, y1, z1, c);
        line(pose, buf, x2, y1, z1, x2, y1, z2, c);
        line(pose, buf, x2, y1, z2, x1, y1, z2, c);
        line(pose, buf, x1, y1, z2, x1, y1, z1, c);
        // top
        line(pose, buf, x1, y2, z1, x2, y2, z1, c);
        line(pose, buf, x2, y2, z1, x2, y2, z2, c);
        line(pose, buf, x2, y2, z2, x1, y2, z2, c);
        line(pose, buf, x1, y2, z2, x1, y2, z1, c);
        // sides
        line(pose, buf, x1, y1, z1, x1, y2, z1, c);
        line(pose, buf, x2, y1, z1, x2, y2, z1, c);
        line(pose, buf, x2, y1, z2, x2, y2, z2, c);
        line(pose, buf, x1, y1, z2, x1, y2, z2, c);
    }

    public static void quadBox(PoseStack.Pose pose, VertexConsumer buf, AABB b, int c) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        // bottom
        buf.addVertex(pose, x1, y1, z1).setColor(c);
        buf.addVertex(pose, x2, y1, z1).setColor(c);
        buf.addVertex(pose, x2, y1, z2).setColor(c);
        buf.addVertex(pose, x1, y1, z2).setColor(c);
        // top
        buf.addVertex(pose, x1, y2, z1).setColor(c);
        buf.addVertex(pose, x1, y2, z2).setColor(c);
        buf.addVertex(pose, x2, y2, z2).setColor(c);
        buf.addVertex(pose, x2, y2, z1).setColor(c);
        // north
        buf.addVertex(pose, x1, y1, z1).setColor(c);
        buf.addVertex(pose, x1, y2, z1).setColor(c);
        buf.addVertex(pose, x2, y2, z1).setColor(c);
        buf.addVertex(pose, x2, y1, z1).setColor(c);
        // east
        buf.addVertex(pose, x2, y1, z1).setColor(c);
        buf.addVertex(pose, x2, y2, z1).setColor(c);
        buf.addVertex(pose, x2, y2, z2).setColor(c);
        buf.addVertex(pose, x2, y1, z2).setColor(c);
        // south
        buf.addVertex(pose, x1, y1, z2).setColor(c);
        buf.addVertex(pose, x2, y1, z2).setColor(c);
        buf.addVertex(pose, x2, y2, z2).setColor(c);
        buf.addVertex(pose, x1, y2, z2).setColor(c);
        // west
        buf.addVertex(pose, x1, y1, z1).setColor(c);
        buf.addVertex(pose, x1, y1, z2).setColor(c);
        buf.addVertex(pose, x1, y2, z2).setColor(c);
        buf.addVertex(pose, x1, y2, z1).setColor(c);
    }

    // ------------------------------------------------------------------
    // Projection
    // ------------------------------------------------------------------

    /**
     * Projects a world position onto the GUI. Returns null when the point is
     * behind the camera or matrices are not available yet.
     */
    public static Vec3 toScreen(Vec3 world) {
        if (!haveMatrices) return null;
        Vec3 cam = cameraPos();
        Vector4f v = new Vector4f((float) (world.x - cam.x), (float) (world.y - cam.y), (float) (world.z - cam.z), 1f);
        VIEW.transform(v);
        PROJ.transform(v);
        if (v.w <= 0.05f) return null;
        float ndcX = v.x / v.w;
        float ndcY = v.y / v.w;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        double sx = (ndcX * 0.5 + 0.5) * w;
        double sy = (1 - (ndcY * 0.5 + 0.5)) * h;
        return new Vec3(sx, sy, v.w);
    }

    // ------------------------------------------------------------------
    // Colors
    // ------------------------------------------------------------------

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static int rainbow(long offsetMs) {
        float hue = ((System.currentTimeMillis() + offsetMs) % 4000L) / 4000f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.65f, 1f) & 0xFFFFFF);
    }

    /** Green → yellow → red depending on 0..1. */
    public static int healthColor(float fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        int r = (int) (255 * Math.min(1, 2 * (1 - fraction)));
        int g = (int) (255 * Math.min(1, 2 * fraction));
        return 0xFF000000 | (r << 16) | (g << 8) | 0x20;
    }
}
