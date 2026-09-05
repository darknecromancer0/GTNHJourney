package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure Group × Order × Latest planner. Input membership is already finalized by native NEI filters. */
public final class JourneySortPlanner {

    private JourneySortPlanner() {}

    public static List<JourneySortEntry> sort(
        List<JourneySortEntry> source,
        JourneyGroupMode groupMode,
        JourneyOrderMode orderMode,
        boolean latest) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        JourneyGroupMode group = groupMode == null ? JourneyGroupMode.NONE : groupMode;
        JourneyOrderMode order = orderMode == null ? JourneyOrderMode.NONE : orderMode;

        List<Bucket> buckets = buckets(source, group);
        Collections.sort(buckets, bucketComparator(group, order, latest));
        List<JourneySortEntry> out = new ArrayList<JourneySortEntry>(source.size());
        for (Bucket bucket : buckets) {
            Collections.sort(bucket.entries, memberComparator(group, order));
            if (latest && group != JourneyGroupMode.NONE) promoteLatestMember(bucket.entries);
            out.addAll(bucket.entries);
        }
        return Collections.unmodifiableList(out);
    }

    private static void promoteLatestMember(List<JourneySortEntry> entries) {
        if (entries == null || entries.size() < 2) return;
        int latestIndex = 0;
        long latestSequence = entries.get(0).activitySequence();
        for (int i = 1; i < entries.size(); i++) {
            long sequence = entries.get(i).activitySequence();
            if (sequence > latestSequence) {
                latestIndex = i;
                latestSequence = sequence;
            }
        }
        if (latestIndex <= 0) return;
        JourneySortEntry latestEntry = entries.remove(latestIndex);
        entries.add(0, latestEntry);
    }

    private static List<Bucket> buckets(List<JourneySortEntry> source, JourneyGroupMode group) {
        Map<String, Bucket> buckets = new LinkedHashMap<String, Bucket>();
        int ordinal = 0;
        for (JourneySortEntry entry : source) {
            if (entry == null) continue;
            String key = groupKey(entry, group, ordinal++);
            Bucket bucket = buckets.get(key);
            if (bucket == null) {
                bucket = new Bucket(key);
                buckets.put(key, bucket);
            }
            bucket.add(entry);
        }
        return new ArrayList<Bucket>(buckets.values());
    }

    private static String groupKey(JourneySortEntry entry, JourneyGroupMode group, int ordinal) {
        switch (group) {
            case NATIVE:
                return "n:" + entry.nativeFamily();
            case MOD:
                return "m:" + entry.modGroup();
            case TYPE:
                return "t:" + entry.typeGroup();
            case KIND:
                return "k:" + entry.kindGroup();
            case NONE:
            default:
                return "i:" + ordinal + ':' + entry.key().toString();
        }
    }

    private static Comparator<Bucket> bucketComparator(
        final JourneyGroupMode group,
        final JourneyOrderMode order,
        final boolean latest) {
        return new Comparator<Bucket>() {
            @Override
            public int compare(Bucket left, Bucket right) {
                if (latest) {
                    int activity = compareLongDesc(left.maxActivity, right.maxActivity);
                    if (activity != 0) return activity;
                }
                int ordered = compareByOrder(left, right, order);
                if (ordered != 0) return ordered;
                if (group == JourneyGroupMode.NATIVE) {
                    int nativeOrder = Integer.compare(left.minNativeIndex, right.minNativeIndex);
                    if (nativeOrder != 0) return nativeOrder;
                }
                int canonical = Integer.compare(left.minCanonicalIndex, right.minCanonicalIndex);
                if (canonical != 0) return canonical;
                return left.key.compareTo(right.key);
            }
        };
    }

    private static int compareByOrder(Bucket left, Bucket right, JourneyOrderMode order) {
        switch (order) {
            case UNLOCK:
                return compareLongDesc(left.maxUnlock, right.maxUnlock);
            case FAVOURITE_ADDED:
                return compareLongDesc(left.maxFavourite, right.maxFavourite);
            case ALPHABETICAL:
                return left.minName.compareTo(right.minName);
            case NONE:
            default:
                return 0;
        }
    }

    private static Comparator<JourneySortEntry> memberComparator(
        final JourneyGroupMode group,
        final JourneyOrderMode order) {
        return new Comparator<JourneySortEntry>() {
            @Override
            public int compare(JourneySortEntry left, JourneySortEntry right) {
                // N without L is the strict family mode: subtype/state order remains exactly native NEI.
                if (group == JourneyGroupMode.NATIVE) return stableNative(left, right);

                if (group != JourneyGroupMode.NONE) {
                    int ordered = compareMembersByOrder(left, right, order);
                    if (ordered != 0) return ordered;
                }
                return stableNative(left, right);
            }
        };
    }

    private static int compareMembersByOrder(
        JourneySortEntry left,
        JourneySortEntry right,
        JourneyOrderMode order) {
        switch (order) {
            case UNLOCK:
                return compareLongDesc(left.unlockSequence(), right.unlockSequence());
            case FAVOURITE_ADDED:
                return compareLongDesc(left.favouriteSequence(), right.favouriteSequence());
            case ALPHABETICAL:
                int name = left.displayName().toLowerCase(Locale.ROOT)
                    .compareTo(right.displayName().toLowerCase(Locale.ROOT));
                if (name != 0) return name;
                return 0;
            case NONE:
            default:
                return 0;
        }
    }

    private static int stableNative(JourneySortEntry left, JourneySortEntry right) {
        int nativeOrder = Integer.compare(left.nativeIndex(), right.nativeIndex());
        if (nativeOrder != 0) return nativeOrder;
        int canonical = Integer.compare(left.canonicalIndex(), right.canonicalIndex());
        if (canonical != 0) return canonical;
        int item = left.key().getItemId().compareTo(right.key().getItemId());
        if (item != 0) return item;
        int meta = Integer.compare(left.key().getMeta(), right.key().getMeta());
        if (meta != 0) return meta;
        return left.key().getCanonicalNbt().compareTo(right.key().getCanonicalNbt());
    }

    private static int compareLongDesc(long left, long right) {
        return left == right ? 0 : (left > right ? -1 : 1);
    }

    private static final class Bucket {
        final String key;
        final List<JourneySortEntry> entries = new ArrayList<JourneySortEntry>();
        long maxUnlock = Long.MIN_VALUE;
        long maxActivity = Long.MIN_VALUE;
        long maxFavourite = Long.MIN_VALUE;
        int minNativeIndex = Integer.MAX_VALUE;
        int minCanonicalIndex = Integer.MAX_VALUE;
        String minName = "\uffff";

        Bucket(String key) {
            this.key = key;
        }

        void add(JourneySortEntry entry) {
            entries.add(entry);
            maxUnlock = Math.max(maxUnlock, entry.unlockSequence());
            maxActivity = Math.max(maxActivity, entry.activitySequence());
            maxFavourite = Math.max(maxFavourite, entry.favouriteSequence());
            minNativeIndex = Math.min(minNativeIndex, entry.nativeIndex());
            minCanonicalIndex = Math.min(minCanonicalIndex, entry.canonicalIndex());
            String normalizedName = entry.displayName().toLowerCase(Locale.ROOT);
            if (normalizedName.compareTo(minName) < 0) minName = normalizedName;
        }
    }
}
