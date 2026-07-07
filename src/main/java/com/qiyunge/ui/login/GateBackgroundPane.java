package com.qiyunge.ui.login;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * 门庭场景：绘制深青背景、云雾远山、古风双门、门框、门匾、门环和门缝金光。
 * 不负责认证逻辑，只负责视觉。
 */
public class GateBackgroundPane extends Pane {

    private final Rectangle leftDoor;
    private final Rectangle rightDoor;
    private final Rectangle centerLight;
    private final Group leftRing;
    private final Group rightRing;
    private final Rectangle glowLayer;

    private final DoubleProperty centerLightWidth = new SimpleDoubleProperty(2);

    public GateBackgroundPane() {
        getStyleClass().add("gate-background");
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // 深色背景
        Rectangle bg = new Rectangle();
        bg.setFill(new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#102A33")),
            new Stop(0.56, Color.web("#183A43")),
            new Stop(1, Color.web("#0F2027"))
        ));
        bg.widthProperty().bind(widthProperty());
        bg.heightProperty().bind(heightProperty());
        bg.setManaged(false);

        // 开门光晕
        glowLayer = new Rectangle();
        glowLayer.setManaged(false);
        glowLayer.setFill(Color.web("#F0D99A", 0.0));
        glowLayer.xProperty().bind(widthProperty().multiply(0.5).subtract(centerLightWidth.multiply(1.2)));
        glowLayer.yProperty().bind(heightProperty().multiply(0.12));
        glowLayer.widthProperty().bind(centerLightWidth.multiply(2.4));
        glowLayer.heightProperty().bind(heightProperty().multiply(0.76));
        glowLayer.setArcWidth(120);
        glowLayer.setArcHeight(120);

        DoubleBinding gateWidth = widthProperty().multiply(0.56);
        DoubleBinding gateHeight = heightProperty().multiply(0.76);
        DoubleBinding gateX = widthProperty().subtract(gateWidth).divide(2);
        DoubleBinding gateY = heightProperty().subtract(gateHeight).divide(2).subtract(6);
        DoubleBinding doorWidth = gateWidth.divide(2);

        // 左门扇
        leftDoor = new Rectangle();
        leftDoor.setFill(Color.web("#203A43"));
        leftDoor.setStroke(Color.web("#345E68"));
        leftDoor.setStrokeWidth(1.2);
        leftDoor.setArcWidth(8);
        leftDoor.setArcHeight(8);
        leftDoor.setManaged(false);
        leftDoor.xProperty().bind(gateX);
        leftDoor.yProperty().bind(gateY.add(34));
        leftDoor.widthProperty().bind(doorWidth);
        leftDoor.heightProperty().bind(gateHeight.subtract(34));

        // 右门扇
        rightDoor = new Rectangle();
        rightDoor.setFill(Color.web("#203A43"));
        rightDoor.setStroke(Color.web("#345E68"));
        rightDoor.setStrokeWidth(1.2);
        rightDoor.setArcWidth(8);
        rightDoor.setArcHeight(8);
        rightDoor.setManaged(false);
        rightDoor.xProperty().bind(gateX.add(doorWidth));
        rightDoor.yProperty().bind(gateY.add(34));
        rightDoor.widthProperty().bind(doorWidth);
        rightDoor.heightProperty().bind(gateHeight.subtract(34));

        // 中央金光
        centerLight = new Rectangle();
        centerLight.setFill(Color.web("#D8B36A", 0.35));
        centerLight.setManaged(false);
        centerLight.xProperty().bind(widthProperty().subtract(centerLightWidth).divide(2));
        centerLight.yProperty().bind(gateY.add(36));
        centerLight.widthProperty().bind(centerLightWidth);
        centerLight.heightProperty().bind(gateHeight.subtract(38));

        Pane backgroundOrnaments = createBackgroundOrnaments();
        Pane gateOrnaments = createGateOrnaments(gateX, gateY, gateWidth, gateHeight, doorWidth);

        leftRing = createDoorRing();
        leftRing.layoutXProperty().bind(widthProperty().multiply(0.5).subtract(42));
        leftRing.layoutYProperty().bind(gateY.add(gateHeight.multiply(0.54)));
        leftRing.translateXProperty().bind(leftDoor.translateXProperty());

        rightRing = createDoorRing();
        rightRing.layoutXProperty().bind(widthProperty().multiply(0.5).add(42));
        rightRing.layoutYProperty().bind(gateY.add(gateHeight.multiply(0.54)));
        rightRing.translateXProperty().bind(rightDoor.translateXProperty());

        getChildren().addAll(
            bg,
            backgroundOrnaments,
            glowLayer,
            leftDoor,
            rightDoor,
            gateOrnaments,
            centerLight,
            leftRing,
            rightRing
        );
    }

    private Pane createBackgroundOrnaments() {
        Pane layer = new Pane();
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.prefWidthProperty().bind(widthProperty());
        layer.prefHeightProperty().bind(heightProperty());

        Ellipse moonGlow = new Ellipse();
        moonGlow.radiusXProperty().bind(widthProperty().multiply(0.14));
        moonGlow.radiusYProperty().bind(widthProperty().multiply(0.14));
        moonGlow.centerXProperty().bind(widthProperty().multiply(0.78));
        moonGlow.centerYProperty().bind(heightProperty().multiply(0.16));
        moonGlow.setFill(Color.web("#FFFFFF", 0.045));

        Circle cloud1 = new Circle();
        cloud1.setRadius(74);
        cloud1.setFill(Color.web("#FFFFFF", 0.035));
        cloud1.centerXProperty().bind(widthProperty().multiply(0.13));
        cloud1.centerYProperty().bind(heightProperty().multiply(0.28));

        Circle cloud2 = new Circle();
        cloud2.setRadius(96);
        cloud2.setFill(Color.web("#FFFFFF", 0.025));
        cloud2.centerXProperty().bind(widthProperty().multiply(0.88));
        cloud2.centerYProperty().bind(heightProperty().multiply(0.40));

        Circle cloud3 = new Circle();
        cloud3.setRadius(52);
        cloud3.setFill(Color.web("#FFFFFF", 0.04));
        cloud3.centerXProperty().bind(widthProperty().multiply(0.20));
        cloud3.centerYProperty().bind(heightProperty().multiply(0.72));

        Circle cloud4 = new Circle();
        cloud4.setRadius(80);
        cloud4.setFill(Color.web("#FFFFFF", 0.022));
        cloud4.centerXProperty().bind(widthProperty().multiply(0.85));
        cloud4.centerYProperty().bind(heightProperty().multiply(0.78));

        Polyline mountainA = new Polyline();
        mountainA.getPoints().addAll(0.0, 0.0, 140.0, -58.0, 260.0, -20.0, 420.0, -84.0, 580.0, -18.0, 760.0, -66.0, 1000.0, -8.0);
        mountainA.translateYProperty().bind(heightProperty().multiply(0.88));
        mountainA.setStroke(Color.web("#FFFFFF", 0.052));
        mountainA.setStrokeWidth(1.1);
        mountainA.setFill(Color.TRANSPARENT);

        Polyline mountainB = new Polyline();
        mountainB.getPoints().addAll(0.0, 0.0, 180.0, -34.0, 340.0, -12.0, 520.0, -54.0, 700.0, -20.0, 860.0, -46.0, 1000.0, -4.0);
        mountainB.translateYProperty().bind(heightProperty().multiply(0.94));
        mountainB.setStroke(Color.web("#FFFFFF", 0.035));
        mountainB.setStrokeWidth(1);
        mountainB.setFill(Color.TRANSPARENT);

        layer.getChildren().addAll(moonGlow, cloud1, cloud2, cloud3, cloud4, mountainA, mountainB);
        return layer;
    }

    private Pane createGateOrnaments(DoubleBinding gateX,
                                     DoubleBinding gateY,
                                     DoubleBinding gateWidth,
                                     DoubleBinding gateHeight,
                                     DoubleBinding doorWidth) {
        Pane layer = new Pane();
        layer.setManaged(false);
        layer.setMouseTransparent(true);
        layer.prefWidthProperty().bind(widthProperty());
        layer.prefHeightProperty().bind(heightProperty());

        Rectangle outerFrame = new Rectangle();
        outerFrame.setManaged(false);
        outerFrame.xProperty().bind(gateX.subtract(16));
        outerFrame.yProperty().bind(gateY.add(18));
        outerFrame.widthProperty().bind(gateWidth.add(32));
        outerFrame.heightProperty().bind(gateHeight.subtract(4));
        outerFrame.setFill(Color.TRANSPARENT);
        outerFrame.setStroke(Color.web("#D8B36A", 0.24));
        outerFrame.setStrokeWidth(1.4);
        outerFrame.setArcWidth(12);
        outerFrame.setArcHeight(12);

        Rectangle innerFrame = new Rectangle();
        innerFrame.setManaged(false);
        innerFrame.xProperty().bind(gateX.add(18));
        innerFrame.yProperty().bind(gateY.add(56));
        innerFrame.widthProperty().bind(gateWidth.subtract(36));
        innerFrame.heightProperty().bind(gateHeight.subtract(76));
        innerFrame.setFill(Color.TRANSPARENT);
        innerFrame.setStroke(Color.web("#FFFFFF", 0.10));
        innerFrame.setStrokeWidth(1);
        innerFrame.setArcWidth(10);
        innerFrame.setArcHeight(10);

        Rectangle plaque = new Rectangle();
        plaque.setManaged(false);
        plaque.xProperty().bind(widthProperty().multiply(0.5).subtract(62));
        plaque.yProperty().bind(gateY.subtract(4));
        plaque.setWidth(124);
        plaque.setHeight(38);
        plaque.setArcWidth(8);
        plaque.setArcHeight(8);
        plaque.setFill(Color.web("#152A31", 0.96));
        plaque.setStroke(Color.web("#D8B36A", 0.42));
        plaque.setStrokeWidth(1);

        Text plaqueText = new Text("栖云阁");
        plaqueText.setManaged(false);
        plaqueText.setFill(Color.web("#D8B36A", 0.92));
        plaqueText.setFont(Font.font("Microsoft YaHei UI", FontWeight.SEMI_BOLD, 18));
        plaqueText.xProperty().bind(widthProperty().multiply(0.5).subtract(27));
        plaqueText.yProperty().bind(gateY.add(21));

        Rectangle leftInner = createDoorInnerFrame();
        leftInner.xProperty().bind(gateX.add(24));
        leftInner.yProperty().bind(gateY.add(68));
        leftInner.widthProperty().bind(doorWidth.subtract(48));
        leftInner.heightProperty().bind(gateHeight.subtract(110));

        Rectangle rightInner = createDoorInnerFrame();
        rightInner.xProperty().bind(gateX.add(doorWidth).add(24));
        rightInner.yProperty().bind(gateY.add(68));
        rightInner.widthProperty().bind(doorWidth.subtract(48));
        rightInner.heightProperty().bind(gateHeight.subtract(110));

        Line leftBeam = createDoorBeam();
        leftBeam.startXProperty().bind(gateX.add(24));
        leftBeam.endXProperty().bind(gateX.add(doorWidth).subtract(24));
        leftBeam.startYProperty().bind(gateY.add(gateHeight.multiply(0.32)));
        leftBeam.endYProperty().bind(leftBeam.startYProperty());

        Line rightBeam = createDoorBeam();
        rightBeam.startXProperty().bind(gateX.add(doorWidth).add(24));
        rightBeam.endXProperty().bind(gateX.add(gateWidth).subtract(24));
        rightBeam.startYProperty().bind(gateY.add(gateHeight.multiply(0.32)));
        rightBeam.endYProperty().bind(rightBeam.startYProperty());

        Line leftLowerBeam = createDoorBeam();
        leftLowerBeam.startXProperty().bind(gateX.add(24));
        leftLowerBeam.endXProperty().bind(gateX.add(doorWidth).subtract(24));
        leftLowerBeam.startYProperty().bind(gateY.add(gateHeight.multiply(0.72)));
        leftLowerBeam.endYProperty().bind(leftLowerBeam.startYProperty());

        Line rightLowerBeam = createDoorBeam();
        rightLowerBeam.startXProperty().bind(gateX.add(doorWidth).add(24));
        rightLowerBeam.endXProperty().bind(gateX.add(gateWidth).subtract(24));
        rightLowerBeam.startYProperty().bind(gateY.add(gateHeight.multiply(0.72)));
        rightLowerBeam.endYProperty().bind(rightLowerBeam.startYProperty());

        Line leftTexture = createDoorTexture();
        leftTexture.startXProperty().bind(gateX.add(doorWidth.multiply(0.34)));
        leftTexture.endXProperty().bind(leftTexture.startXProperty());
        leftTexture.startYProperty().bind(gateY.add(76));
        leftTexture.endYProperty().bind(gateY.add(gateHeight).subtract(42));

        Line rightTexture = createDoorTexture();
        rightTexture.startXProperty().bind(gateX.add(doorWidth).add(doorWidth.multiply(0.66)));
        rightTexture.endXProperty().bind(rightTexture.startXProperty());
        rightTexture.startYProperty().bind(gateY.add(76));
        rightTexture.endYProperty().bind(gateY.add(gateHeight).subtract(42));

        Rectangle baseLine = new Rectangle();
        baseLine.setManaged(false);
        baseLine.xProperty().bind(gateX.subtract(28));
        baseLine.yProperty().bind(gateY.add(gateHeight).add(16));
        baseLine.widthProperty().bind(gateWidth.add(56));
        baseLine.setHeight(2);
        baseLine.setFill(Color.web("#D8B36A", 0.20));

        Group leftDoorDetails = new Group(leftInner, leftBeam, leftLowerBeam, leftTexture);
        leftDoorDetails.setManaged(false);
        leftDoorDetails.setMouseTransparent(true);
        leftDoorDetails.translateXProperty().bind(leftDoor.translateXProperty());

        Group rightDoorDetails = new Group(rightInner, rightBeam, rightLowerBeam, rightTexture);
        rightDoorDetails.setManaged(false);
        rightDoorDetails.setMouseTransparent(true);
        rightDoorDetails.translateXProperty().bind(rightDoor.translateXProperty());

        layer.getChildren().addAll(
            outerFrame, innerFrame, plaque, plaqueText,
            leftDoorDetails, rightDoorDetails,
            baseLine
        );
        return layer;
    }

    private Rectangle createDoorInnerFrame() {
        Rectangle rect = new Rectangle();
        rect.setManaged(false);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.web("#FFFFFF", 0.085));
        rect.setStrokeWidth(1);
        rect.setArcWidth(7);
        rect.setArcHeight(7);
        return rect;
    }

    private Line createDoorBeam() {
        Line line = new Line();
        line.setManaged(false);
        line.setStroke(Color.web("#152A31", 0.50));
        line.setStrokeWidth(2);
        return line;
    }

    private Line createDoorTexture() {
        Line line = new Line();
        line.setManaged(false);
        line.setStroke(Color.web("#FFFFFF", 0.045));
        line.setStrokeWidth(1);
        return line;
    }

    private Group createDoorRing() {
        Circle ring = new Circle(0, 0, 12);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#D8B36A", 0.46));
        ring.setStrokeWidth(1.8);

        Circle stud = new Circle(0, 0, 4);
        stud.setFill(Color.web("#D8B36A", 0.36));

        Line drop = new Line(0, 10, 0, 22);
        drop.setStroke(Color.web("#D8B36A", 0.28));
        drop.setStrokeWidth(1.4);

        Group group = new Group(ring, stud, drop);
        group.setManaged(false);
        group.setMouseTransparent(true);
        return group;
    }

    public Rectangle getLeftDoor() { return leftDoor; }
    public Rectangle getRightDoor() { return rightDoor; }
    public Group getLeftRing() { return leftRing; }
    public Group getRightRing() { return rightRing; }
    public Rectangle getGlowLayer() { return glowLayer; }
    public DoubleProperty centerLightWidthProperty() { return centerLightWidth; }
}
