package com.mery.npc.model;

import org.bukkit.util.EulerAngle;

import java.util.HashMap;
import java.util.Map;

public class NPCPose {

    private EulerAngle head = EulerAngle.ZERO;
    private EulerAngle body = EulerAngle.ZERO;
    private EulerAngle leftArm = EulerAngle.ZERO;
    private EulerAngle rightArm = EulerAngle.ZERO;
    private EulerAngle leftLeg = EulerAngle.ZERO;
    private EulerAngle rightLeg = EulerAngle.ZERO;
    private String presetName = "STANDING";

    public NPCPose() {
        applyPreset("STANDING");
    }

    public void applyPreset(String preset) {
        this.presetName = preset.toUpperCase();
        switch (this.presetName) {
            case "GUARD":
                // Ready combat stance: sword angled, shield forward
                this.head = new EulerAngle(Math.toRadians(5), Math.toRadians(-5), 0);
                this.body = new EulerAngle(0, Math.toRadians(-10), 0);
                this.rightArm = new EulerAngle(Math.toRadians(-35), Math.toRadians(-15), Math.toRadians(15)); // sword
                this.leftArm = new EulerAngle(Math.toRadians(-50), Math.toRadians(25), Math.toRadians(-10)); // shield
                this.rightLeg = new EulerAngle(Math.toRadians(10), 0, Math.toRadians(5));
                this.leftLeg = new EulerAngle(Math.toRadians(-15), 0, Math.toRadians(-5));
                break;

            case "ATTACK":
                // Slashing sword attack
                this.head = new EulerAngle(Math.toRadians(10), 0, 0);
                this.body = new EulerAngle(Math.toRadians(15), Math.toRadians(20), 0);
                this.rightArm = new EulerAngle(Math.toRadians(-120), Math.toRadians(30), Math.toRadians(25));
                this.leftArm = new EulerAngle(Math.toRadians(20), Math.toRadians(-20), Math.toRadians(-20));
                this.rightLeg = new EulerAngle(Math.toRadians(25), 0, 0);
                this.leftLeg = new EulerAngle(Math.toRadians(-30), 0, 0);
                break;

            case "DEFEND":
                // Blocking stance: shield directly in front
                this.head = new EulerAngle(Math.toRadians(10), 0, 0);
                this.body = new EulerAngle(Math.toRadians(10), 0, 0);
                this.leftArm = new EulerAngle(Math.toRadians(-80), Math.toRadians(40), Math.toRadians(-20));
                this.rightArm = new EulerAngle(Math.toRadians(-20), Math.toRadians(-20), Math.toRadians(10));
                this.rightLeg = new EulerAngle(Math.toRadians(15), 0, 0);
                this.leftLeg = new EulerAngle(Math.toRadians(-15), 0, 0);
                break;

            case "SALUTE":
                // Right hand raised to forehead
                this.head = new EulerAngle(0, 0, 0);
                this.body = new EulerAngle(0, 0, 0);
                this.rightArm = new EulerAngle(Math.toRadians(-110), Math.toRadians(-40), Math.toRadians(50));
                this.leftArm = new EulerAngle(0, 0, Math.toRadians(-5));
                this.rightLeg = EulerAngle.ZERO;
                this.leftLeg = EulerAngle.ZERO;
                break;

            case "POINTING":
                // Pointing right arm forward
                this.head = new EulerAngle(0, 0, 0);
                this.body = new EulerAngle(0, 0, 0);
                this.rightArm = new EulerAngle(Math.toRadians(-90), 0, 0);
                this.leftArm = new EulerAngle(0, 0, Math.toRadians(-5));
                this.rightLeg = EulerAngle.ZERO;
                this.leftLeg = EulerAngle.ZERO;
                break;

            case "CHEERING":
                // Both arms raised high in victory
                this.head = new EulerAngle(Math.toRadians(-20), 0, 0);
                this.body = new EulerAngle(0, 0, 0);
                this.rightArm = new EulerAngle(Math.toRadians(-160), Math.toRadians(-20), Math.toRadians(20));
                this.leftArm = new EulerAngle(Math.toRadians(-160), Math.toRadians(20), Math.toRadians(-20));
                this.rightLeg = EulerAngle.ZERO;
                this.leftLeg = EulerAngle.ZERO;
                break;

            case "ZOMBIE":
                // Both arms straight forward
                this.head = new EulerAngle(0, 0, 0);
                this.body = new EulerAngle(0, 0, 0);
                this.rightArm = new EulerAngle(Math.toRadians(-90), 0, 0);
                this.leftArm = new EulerAngle(Math.toRadians(-90), 0, 0);
                this.rightLeg = EulerAngle.ZERO;
                this.leftLeg = EulerAngle.ZERO;
                break;

            case "SITTING":
                // Sitting pose
                this.head = EulerAngle.ZERO;
                this.body = EulerAngle.ZERO;
                this.rightArm = new EulerAngle(Math.toRadians(-20), 0, 0);
                this.leftArm = new EulerAngle(Math.toRadians(-20), 0, 0);
                this.rightLeg = new EulerAngle(Math.toRadians(-90), Math.toRadians(15), 0);
                this.leftLeg = new EulerAngle(Math.toRadians(-90), Math.toRadians(-15), 0);
                break;

            case "STANDING":
            default:
                this.presetName = "STANDING";
                this.head = EulerAngle.ZERO;
                this.body = EulerAngle.ZERO;
                this.rightArm = EulerAngle.ZERO;
                this.leftArm = EulerAngle.ZERO;
                this.rightLeg = EulerAngle.ZERO;
                this.leftLeg = EulerAngle.ZERO;
                break;
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("preset", presetName);
        map.put("head", toList(head));
        map.put("body", toList(body));
        map.put("leftArm", toList(leftArm));
        map.put("rightArm", toList(rightArm));
        map.put("leftLeg", toList(leftLeg));
        map.put("rightLeg", toList(rightLeg));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static NPCPose deserialize(Map<String, Object> map) {
        NPCPose pose = new NPCPose();
        if (map.containsKey("preset")) {
            pose.applyPreset((String) map.get("preset"));
        }
        if (map.containsKey("head")) pose.setHead(fromList(map.get("head")));
        if (map.containsKey("body")) pose.setBody(fromList(map.get("body")));
        if (map.containsKey("leftArm")) pose.setLeftArm(fromList(map.get("leftArm")));
        if (map.containsKey("rightArm")) pose.setRightArm(fromList(map.get("rightArm")));
        if (map.containsKey("leftLeg")) pose.setLeftLeg(fromList(map.get("leftLeg")));
        if (map.containsKey("rightLeg")) pose.setRightLeg(fromList(map.get("rightLeg")));
        return pose;
    }

    private static double[] toList(EulerAngle angle) {
        return new double[]{Math.toDegrees(angle.getX()), Math.toDegrees(angle.getY()), Math.toDegrees(angle.getZ())};
    }

    private static EulerAngle fromList(Object obj) {
        if (obj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) obj;
            if (list.size() >= 3) {
                double x = ((Number) list.get(0)).doubleValue();
                double y = ((Number) list.get(1)).doubleValue();
                double z = ((Number) list.get(2)).doubleValue();
                return new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
            }
        } else if (obj instanceof double[]) {
            double[] arr = (double[]) obj;
            if (arr.length >= 3) {
                return new EulerAngle(Math.toRadians(arr[0]), Math.toRadians(arr[1]), Math.toRadians(arr[2]));
            }
        }
        return EulerAngle.ZERO;
    }

    // Getters and Setters
    public EulerAngle getHead() { return head; }
    public void setHead(EulerAngle head) { this.head = head; this.presetName = "CUSTOM"; }

    public EulerAngle getBody() { return body; }
    public void setBody(EulerAngle body) { this.body = body; this.presetName = "CUSTOM"; }

    public EulerAngle getLeftArm() { return leftArm; }
    public void setLeftArm(EulerAngle leftArm) { this.leftArm = leftArm; this.presetName = "CUSTOM"; }

    public EulerAngle getRightArm() { return rightArm; }
    public void setRightArm(EulerAngle rightArm) { this.rightArm = rightArm; this.presetName = "CUSTOM"; }

    public EulerAngle getLeftLeg() { return leftLeg; }
    public void setLeftLeg(EulerAngle leftLeg) { this.leftLeg = leftLeg; this.presetName = "CUSTOM"; }

    public EulerAngle getRightLeg() { return rightLeg; }
    public void setRightLeg(EulerAngle rightLeg) { this.rightLeg = rightLeg; this.presetName = "CUSTOM"; }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }
}

