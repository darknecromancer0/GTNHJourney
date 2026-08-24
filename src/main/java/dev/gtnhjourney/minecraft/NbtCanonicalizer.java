package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;

/**
 * Produces a deterministic, type-aware identity string for Minecraft 1.7.10 NBT.
 *
 * <p>
 * This is deliberately not Mojangson. Retrieval stores the original NBT payload separately; this string exists
 * only for equality/hash identity. Compound keys are sorted and list order is preserved.
 * </p>
 */
public final class NbtCanonicalizer {

    /** Well below vanilla's read limit, but vastly above any sane item-state nesting used by GTNH mods. */
    private static final int MAX_CANONICAL_DEPTH = 128;

    private NbtCanonicalizer() {}

    public interface KeyFilter {

        boolean include(String parentPath, String key);
    }

    private static final KeyFilter INCLUDE_ALL = new KeyFilter() {

        @Override
        public boolean include(String parentPath, String key) {
            return true;
        }
    };

    public static String canonicalize(NBTTagCompound tag) {
        return canonicalize(tag, INCLUDE_ALL);
    }

    public static String canonicalize(NBTTagCompound tag, KeyFilter filter) {
        if (tag == null) return "";
        try {
            return canonicalizeTag(tag, filter == null ? INCLUDE_ALL : filter, "", 0);
        } catch (StackOverflowError tooDeepForVanillaCopy) {
            throw new IllegalArgumentException(
                "NBT nesting overflowed Journey canonicalization safety limits",
                tooDeepForVanillaCopy);
        }
    }

    static String canonicalizeTag(NBTBase tag) {
        try {
            return canonicalizeTag(tag, INCLUDE_ALL, "", 0);
        } catch (StackOverflowError tooDeepForVanillaCopy) {
            throw new IllegalArgumentException(
                "NBT nesting overflowed Journey canonicalization safety limits",
                tooDeepForVanillaCopy);
        }
    }

    @SuppressWarnings("unchecked")
    static String canonicalizeTag(NBTBase tag, KeyFilter filter, String path, int depth) {
        if (tag == null) return "0:null";
        if (depth > MAX_CANONICAL_DEPTH) {
            throw new IllegalArgumentException("NBT nesting exceeds Journey safety limit of " + MAX_CANONICAL_DEPTH);
        }

        if (tag.getId() == 10 && tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            Set<String> rawKeys = (Set<String>) compound.func_150296_c();
            List<String> keys = new ArrayList<String>(rawKeys);
            Collections.sort(keys);

            StringBuilder out = new StringBuilder("10{");
            for (String key : keys) {
                if (!filter.include(path, key)) continue;
                out.append(key.length())
                    .append(':')
                    .append(key)
                    .append('=');
                String childPath = path.length() == 0 ? key : path + "." + key;
                out.append(canonicalizeTag(compound.getTag(key), filter, childPath, depth + 1))
                    .append(';');
            }
            return out.append('}')
                .toString();
        }

        if (tag.getId() == 9 && tag instanceof NBTTagList) {
            // 1.7.10 does not expose a generic indexed getter. Work on a deep copy and pop from the tail so the
            // backing ArrayList stays O(n) overall instead of repeatedly shifting the entire list with removeTag(0).
            NBTTagList copy = (NBTTagList) tag.copy();
            List<String> reversed = new ArrayList<String>(copy.tagCount());
            while (copy.tagCount() > 0) {
                reversed.add(canonicalizeTag(copy.removeTag(copy.tagCount() - 1), filter, path + "[]", depth + 1));
            }
            StringBuilder out = new StringBuilder("9[");
            for (int i = reversed.size() - 1; i >= 0; i--) out.append(reversed.get(i))
                .append(';');
            return out.append(']')
                .toString();
        }

        // Vanilla 1.7.10 NBTTagByteArray#toString only reports "[N bytes]", so using toString here would collapse
        // distinct payloads of equal length. Int arrays are encoded explicitly for the same fail-closed reason.
        if (tag.getId() == 7 && tag instanceof NBTTagByteArray) {
            byte[] values = ((NBTTagByteArray) tag).func_150292_c();
            StringBuilder out = new StringBuilder("7[").append(values.length)
                .append(':');
            for (byte value : values) out.append((int) value)
                .append(',');
            return out.append(']')
                .toString();
        }
        if (tag.getId() == 11 && tag instanceof NBTTagIntArray) {
            int[] values = ((NBTTagIntArray) tag).func_150302_c();
            StringBuilder out = new StringBuilder("11[").append(values.length)
                .append(':');
            for (int value : values) out.append(value)
                .append(',');
            return out.append(']')
                .toString();
        }

        return Byte.toString(tag.getId()) + ':' + tag.toString();
    }
}
