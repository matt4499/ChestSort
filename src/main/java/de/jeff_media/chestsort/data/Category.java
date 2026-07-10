package de.jeff_media.chestsort.data;

import de.jeff_media.chestsort.utils.TypeMatchPositionPair;

public final class Category implements Comparable<Category> {

    public final String name;
    public final TypeMatchPositionPair[] typeMatches;
    private boolean sticky = false;

    public Category(String name, TypeMatchPositionPair[] typeMatches) {
        this.name = name;
        this.typeMatches = typeMatches;
    }

    public void setSticky() {
        this.sticky = true;
    }

    public boolean isSticky() {
        return sticky;
    }

    public short matches(String itemName) {
        for (TypeMatchPositionPair typeMatch : typeMatches) {
            String pattern = typeMatch.typeMatch();
            boolean wildcardBefore = pattern.startsWith("*");
            boolean wildcardAfter = pattern.endsWith("*");

            if (wildcardBefore) {
                pattern = pattern.substring(1);
            }
            if (wildcardAfter) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }

            boolean matches;
            if (wildcardBefore && wildcardAfter) {
                matches = itemName.contains(pattern);
            } else if (wildcardBefore) {
                matches = itemName.endsWith(pattern);
            } else if (wildcardAfter) {
                matches = itemName.startsWith(pattern);
            } else {
                matches = itemName.equalsIgnoreCase(pattern);
            }

            if (matches) {
                return typeMatch.position();
            }
        }
        return 0;
    }

    @Override
    public int compareTo(Category other) {
        return name.compareTo(other.name);
    }
}
