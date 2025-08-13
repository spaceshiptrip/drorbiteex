/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.drorbiteex.fxml;

import eu.dariolucia.drorbiteex.model.ModelManager;
import eu.dariolucia.drorbiteex.model.orbit.IOrbitListener;
import eu.dariolucia.drorbiteex.model.orbit.Orbit;
import eu.dariolucia.drorbiteex.model.orbit.OrbitManager;
import eu.dariolucia.drorbiteex.model.orbit.SpacecraftPosition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.StrokeType;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OrbitGraphics implements IOrbitListener {

    private final ModelManager manager;
    private final Orbit obj;

    private final SimpleBooleanProperty visibleProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    private Group graphicItem;
    private Text textItem;
    private Box scItem;
    private Group groupItem;

    public OrbitGraphics(ModelManager manager, Orbit obj) {
        this.manager = manager;
        this.obj = obj;
        this.obj.addListener(this);
        this.visibleProperty.set(obj.isVisible());
        this.visibleProperty.addListener((source,oldV,newV) -> BackgroundThread.runLater(() -> obj.setVisible(newV)));
        this.selectedProperty.addListener((source,oldV,newV) -> updateOrbitColor(newV));
    }

    private void updateOrbitColor(boolean selected) {
        Color c = selected ? Color.valueOf(obj.getColor()).brighter().brighter() : Color.valueOf(obj.getColor());
        updateElementsColor(c);
    }

    private void updateElementsColor(Color c) {
        PhongMaterial pm = new PhongMaterial(c);
        if(!this.graphicItem.getChildren().isEmpty()) {
            Group orbitGroup = (Group) this.graphicItem.getChildren().get(0);
            for(Node n : orbitGroup.getChildren()) {
                Cylinder cil = (Cylinder) n;
                cil.setMaterial(pm);
            }
        }
        this.scItem.setMaterial(pm);
    }

    public SimpleBooleanProperty visibleProperty() { return visibleProperty; }

    public SimpleBooleanProperty selectedProperty() { return selectedProperty; }

    public String getName() { return obj.getName(); }

    public final Group createGraphicItem() {
        if(this.groupItem != null) {
            return groupItem;
        }
        List<Node> toAdd = constructGraphicItems();
        updateGraphicItems(true);
        this.groupItem = new Group(toAdd);
        this.groupItem.visibleProperty().bind(this.visibleProperty);
        return this.groupItem;
    }

    public Group getGraphicItem() { return graphicItem; }

    private void updateGraphicItems(boolean renderTrajectory) {
        if(renderTrajectory) {
            renderTrajectory();
        }
        renderSpacecraftLocation();
    }

    private void renderTrajectory() {
        Color c = Color.valueOf(obj.getColor());
        if(selectedProperty.get()) {
            c = c.brighter().brighter();
        }
        // Draw trajectory
        List<SpacecraftPosition> trajectory = obj.getSpacecraftPositions();
        // Transform all points to line
        List<Point3D> scPoints = trajectory.stream().map(this::transform).collect(Collectors.toList());
        this.graphicItem.getChildren().clear();
        this.graphicItem.getChildren().add(DrawingUtils.createLine(scPoints, c));
    }

    private void renderSpacecraftLocation() {
        Color c = Color.valueOf(obj.getColor());
        if(selectedProperty.get()) {
            c = c.brighter().brighter();
        }
        // Draw SC position
        SpacecraftPosition currentPosition = obj.getCurrentSpacecraftPosition();
        if(currentPosition == null) {
            return;
        }
        Point3D scLocation = transform(currentPosition);
        this.scItem.setMaterial(new PhongMaterial(c));
        this.scItem.getTransforms().clear();
        this.scItem.getTransforms().add(new Translate(scLocation.getX(), scLocation.getY(), scLocation.getZ()));

        // Set spacecraft text where it is now (3D view label)
        Transform result = new Translate(scLocation.getX() * 1.05, scLocation.getY() * 1.05, scLocation.getZ() * 1.05);
        result = result.createConcatenation(new Rotate(Math.toDegrees(currentPosition.getLatLonHeight().getLongitude()), new Point3D(0, -1, 0)));
        this.textItem.getTransforms().clear();
        this.textItem.getTransforms().add(result);
        this.textItem.setText(obj.getName());
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.BLACK);
    }

    private Point3D transform(SpacecraftPosition ss) {
        Vector3D position = ss.getPositionVector();
        // ECEF to screen
        return new Point3D(position.getY() * DrawingUtils.EARTH_SCALE_FACTOR,
                - position.getZ() * DrawingUtils.EARTH_SCALE_FACTOR,
                - position.getX() * DrawingUtils.EARTH_SCALE_FACTOR);
    }

    private List<Node> constructGraphicItems() {
        // Trajectory object
        this.graphicItem = new Group();
        // Spacecraft object
        this.scItem = new Box(15,15,15);
        // Spacecraft text (3D view)
        this.textItem = new Text(0, 0, obj.getName());

        // Make the 3D label readable
        this.textItem.setFont(Font.font("System", FontWeight.BOLD, 14));
        this.textItem.setFill(Color.WHITE);
        this.textItem.setStroke(Color.BLACK);
        this.textItem.setStrokeWidth(2.0);
        this.textItem.setStrokeType(StrokeType.OUTSIDE);

        return Arrays.asList(graphicItem, scItem, textItem);
    }

    public void draw(GraphicsContext gc, ViewBox widgetViewport, ViewBox latLonViewport, boolean isSelected) {
        // Only draw if the orbit is flagged visible
        if (!obj.isVisible()) {
            return;
        }

        // Grab all precomputed spacecraft positions forming the 2D ground-track
        List<SpacecraftPosition> spacecraftPositions = obj.getSpacecraftPositions();

        // Convert positions to (lat, lon) pairs in degrees for easy 2D projection
        List<double[]> latLonPoints = spacecraftPositions.stream()
                .map(o -> new double[] {
                        Math.toDegrees(o.getLatLonHeight().getLatitude()),
                        Math.toDegrees(o.getLatLonHeight().getLongitude())
                })
                .collect(Collectors.toList());

        // Pick color: brighten if this orbit is selected to make it pop
        if (!isSelected) {
            gc.setStroke(Color.valueOf(obj.getColor()));
            gc.setFill(gc.getStroke());
            gc.setLineWidth(1.5);
        } else {
            gc.setStroke(Color.valueOf(obj.getColor()).brighter().brighter());
            gc.setFill(gc.getStroke());
            gc.setLineWidth(3.5);
        }

        // Draw the ground-track polyline (with gap handling across large longitude jumps)
        if (!latLonPoints.isEmpty()) {
            double[] previousPoint = latLonPoints.get(0);
            double[] start = DrawingUtils.mapToWidgetCoordinates(
                    previousPoint[0], previousPoint[1], widgetViewport, latLonViewport);

            gc.beginPath();
            gc.moveTo(start[0], start[1]);

            for (int i = 1; i < latLonPoints.size(); ++i) {
                double[] nextPoint = latLonPoints.get(i);
                double[] p2 = DrawingUtils.mapToWidgetCoordinates(
                        nextPoint[0], nextPoint[1], widgetViewport, latLonViewport);

                boolean bigJump = Math.abs(nextPoint[1] - previousPoint[1]) > 45;
                if (bigJump) {
                    gc.moveTo(p2[0], p2[1]); // avoid line through the map edge
                } else {
                    gc.lineTo(p2[0], p2[1]);
                }
                previousPoint = nextPoint;
            }

            gc.stroke();
            gc.closePath();
        }

        // If we don’t have a current position, we’re done
        SpacecraftPosition current = obj.getCurrentSpacecraftPosition();
        if (current == null) {
            return;
        }

        // Fetch current lat/lon for the spacecraft marker
        GeodeticPoint scLatLon = current.getLatLonHeight();
        if (scLatLon == null) {
            return; // safety check
        }

        // Project the spacecraft’s current lat/lon into canvas coordinates
        double[] scCenter = DrawingUtils.mapToWidgetCoordinates(
                Math.toDegrees(scLatLon.getLatitude()),
                Math.toDegrees(scLatLon.getLongitude()),
                widgetViewport,
                latLonViewport
        );

        // Marker size
        double baseSize = 8.0;
        double size = isSelected ? 2 * baseSize : baseSize;

        // Draw the filled spacecraft box
        gc.fillRect(scCenter[0] - size / 2.0, scCenter[1] - size / 2.0, size, size);

        // Outline
        gc.setLineWidth(isSelected ? 2.0 : 1.5);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(scCenter[0] - size / 2.0, scCenter[1] - size / 2.0, size, size);

        // --- Draw the spacecraft name with bold font, clamped to the MAP viewport (with shadow margin) ---
        double fontSize = isSelected ? 16 : 14;
        Font labelFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, fontSize);
        gc.setFont(labelFont);

        // Initial label position (above the marker)
        double labelX = scCenter[0];
        double labelY = scCenter[1] - size / 2.0 - 2;

        // Measure text
        double[] textWH = measureText(labelFont, obj.getName());
        double textW = textWH[0];
        double textH = textWH[1];

        // Resolve the map viewport pixel bounds from widgetViewport (supports multiple APIs; falls back to canvas)
        double[] bounds = viewportBounds(widgetViewport, gc);
        double mapLeft   = bounds[0];
        double mapTop    = bounds[1];
        double mapRight  = bounds[2];
        double mapBottom = bounds[3];

        // Extra margin to account for the 8-direction text shadow/outline (so we move BEFORE first/last letter clips)
        double shadowRadius = 10.0;   // was 1.5

        double[] clamped = clampLabelToRectWithinMap(
                labelX, labelY, textW, textH,
                mapLeft, mapTop, mapRight, mapBottom,
                /*markerSizePx=*/size,
                /*shadowRadiusPx=*/shadowRadius
        );
        labelX = clamped[0];
        labelY = clamped[1];

        // Outline (simple 8-direction shadow)
        gc.setFill(Color.BLACK);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    gc.fillText(obj.getName(), labelX + dx, labelY + dy);
                }
            }
        }

        // Foreground text
        gc.setFill(Color.WHITE);
        gc.fillText(obj.getName(), labelX, labelY);
    }

    // ----- Helpers -----

    private static double[] measureText(Font font, String text) {
        Text t = new Text(text);
        t.setFont(font);
        return new double[] { t.getLayoutBounds().getWidth(), t.getLayoutBounds().getHeight() };
    }

    /**
     * Try to extract a numeric property via a list of method names.
     */
    private static double tryInvokeNumber(Object o, String... methods) {
        for (String m : methods) {
            try {
                Method mm = o.getClass().getMethod(m);
                Object v = mm.invoke(o);
                if (v instanceof Number) return ((Number) v).doubleValue();
            } catch (Exception ignored) { }
        }
        return Double.NaN;
    }

    /**
     * Try to extract a numeric field via a list of field names.
     */
    private static double tryFieldNumber(Object o, String... fields) {
        for (String f : fields) {
            try {
                Field ff = o.getClass().getDeclaredField(f);
                ff.setAccessible(true);
                Object v = ff.get(o);
                if (v instanceof Number) return ((Number) v).doubleValue();
            } catch (Exception ignored) { }
        }
        return Double.NaN;
    }

    /**
     * Gets viewport pixel bounds [left, top, right, bottom] from widgetViewport.
     * Supports common APIs:
     *  - getX/getY/getWidth/getHeight
     *  - getMinX/getMinY/getMaxX/getMaxY
     *  - fields: x,y,width,height or minX,minY,maxX,maxY
     * Falls back to canvas bounds if unavailable.
     */
    private static double[] viewportBounds(Object widgetViewport, GraphicsContext gc) {
        // Preferred: explicit min/max
        double minX = tryInvokeNumber(widgetViewport, "getMinX");
        if (Double.isNaN(minX)) minX = tryFieldNumber(widgetViewport, "minX");

        double minY = tryInvokeNumber(widgetViewport, "getMinY");
        if (Double.isNaN(minY)) minY = tryFieldNumber(widgetViewport, "minY");

        double maxX = tryInvokeNumber(widgetViewport, "getMaxX");
        if (Double.isNaN(maxX)) maxX = tryFieldNumber(widgetViewport, "maxX");

        double maxY = tryInvokeNumber(widgetViewport, "getMaxY");
        if (Double.isNaN(maxY)) maxY = tryFieldNumber(widgetViewport, "maxY");

        // If min/max missing, try x/y + width/height
        if (Double.isNaN(minX)) {
            minX = tryInvokeNumber(widgetViewport, "getX");
            if (Double.isNaN(minX)) minX = tryFieldNumber(widgetViewport, "x");
        }
        if (Double.isNaN(minY)) {
            minY = tryInvokeNumber(widgetViewport, "getY");
            if (Double.isNaN(minY)) minY = tryFieldNumber(widgetViewport, "y");
        }
        if (Double.isNaN(maxX)) {
            double w = tryInvokeNumber(widgetViewport, "getWidth");
            if (Double.isNaN(w)) w = tryFieldNumber(widgetViewport, "width");
            if (!Double.isNaN(minX) && !Double.isNaN(w)) maxX = minX + w;
        }
        if (Double.isNaN(maxY)) {
            double h = tryInvokeNumber(widgetViewport, "getHeight");
            if (Double.isNaN(h)) h = tryFieldNumber(widgetViewport, "height");
            if (!Double.isNaN(minY) && !Double.isNaN(h)) maxY = minY + h;
        }

        // Fallback to canvas bounds if still unknown
        if (Double.isNaN(minX)) minX = 0;
        if (Double.isNaN(minY)) minY = 0;
        if (Double.isNaN(maxX)) maxX = gc.getCanvas().getWidth();
        if (Double.isNaN(maxY)) maxY = gc.getCanvas().getHeight();

        return new double[] { minX, minY, maxX, maxY };
    }

    /**
     * Clamp a label so it stays fully inside the map rectangle (including shadow).
     * (labelX,labelY) is the Canvas baseline point for fillText.
     * Strategy:
     *  - Prefer above; if top clips, flip below.
     *  - Clamp horizontally so left/right never clip (text + shadow).
     *  - Clamp vertically to keep all glyphs visible (text + shadow).
     */
    private static double[] clampLabelToRectWithinMap(double labelX, double labelY, double textW, double textH,
                                                      double left, double top, double right, double bottom,
                                                      double markerSizePx, double shadowRadiusPx) {
        // Base padding plus extra for the 8-direction shadow
        double pad = 6.0 + shadowRadiusPx;

        // Start centered over labelX
        double drawX = labelX - (textW / 2.0);
        double drawY = labelY; // baseline

        // Approximate ascent/descent
        double approxAscent = 0.8 * textH;
        double descent = textH - approxAscent;

        // If above would clip (accounting for shadow), place below the marker
        if (drawY - approxAscent - shadowRadiusPx < top + pad) {
            drawY = labelY + markerSizePx + pad + textH; // below
        }

        // Horizontal clamp (keep fully visible left/right incl. shadow)
        if (drawX - shadowRadiusPx < left + pad) {
            drawX = left + pad + shadowRadiusPx;
        }
        if (drawX + textW + shadowRadiusPx > right - pad) {
            drawX = right - pad - shadowRadiusPx - textW;
        }

        // Vertical clamp (keep fully visible top/bottom incl. shadow)
        if (drawY - approxAscent - shadowRadiusPx < top + pad) {
            drawY = top + pad + approxAscent + shadowRadiusPx;
        }
        if (drawY + descent + shadowRadiusPx > bottom - pad) {
            drawY = bottom - pad - shadowRadiusPx - descent;
        }

        // Convert back to baseline-x
        double baselineX = drawX + (textW / 2.0);
        return new double[] { baselineX, drawY };
    }

    // ----- Lifecycle & listener plumbing -----

    public final void dispose() {
        this.groupItem.visibleProperty().unbind();
        this.graphicItem = null;
        this.textItem = null;
        this.scItem = null;
        this.groupItem.getChildren().clear();
        this.groupItem = null;
    }

    @Override
    public void orbitAdded(OrbitManager manager, Orbit orbit) {
        // No-op
    }

    @Override
    public void orbitRemoved(OrbitManager manager, Orbit orbit) {
        if(orbit.equals(this.obj)) {
            Platform.runLater(this::dispose);
        }
    }

    @Override
    public void orbitModelDataUpdated(Orbit orbit, List<SpacecraftPosition> spacecraftPositions, SpacecraftPosition currentPosition) {
        if(orbit.equals(this.obj)) {
            Platform.runLater(() -> updateGraphicItems(true));
        }
    }

    @Override
    public void spacecraftPositionUpdated(Orbit orbit, SpacecraftPosition currentPosition) {
        if(orbit.equals(this.obj)) {
            Platform.runLater(() -> updateGraphicItems(false));
        }
    }

    public Orbit getOrbit() { return obj; }

    @Override
    public String toString() { return this.obj.getName(); }
}

