package ru.mirea.vcv.procedural;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class InstanceAndPlaybackTest {

    @Test
    public void viewersFillInstancesUpToCapacity() {
        List<Model.HallInstance> halls = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            InstanceProcedures.assignViewer(halls, 50);
        }
        assertEquals(3, halls.size());
        assertEquals(50, halls.get(0).viewers);
        assertEquals(50, halls.get(1).viewers);
        assertEquals(20, halls.get(2).viewers);
        assertEquals(120, InstanceProcedures.totalViewers(halls));
    }

    @Test
    public void newViewerGoesToFullestInstanceWithFreeSeat() {
        List<Model.HallInstance> halls = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            InstanceProcedures.assignViewer(halls, 50);
        }
        InstanceProcedures.releaseViewer(halls, 1);
        InstanceProcedures.releaseViewer(halls, 1);
        InstanceProcedures.releaseViewer(halls, 1);
        assertEquals(1, InstanceProcedures.assignViewer(halls, 50));
        assertEquals(48, halls.get(0).viewers);
        assertEquals(3, halls.size());
    }

    @Test
    public void playbackPositionAndPhase() {
        long start = 1_000_000;
        long dur = 5_400_000;
        assertEquals(PlaybackProcedures.BEFORE_START, PlaybackProcedures.phase(start - 1, start, dur));
        assertEquals(0, PlaybackProcedures.playbackPosition(start - 60_000, start, dur));
        assertEquals(PlaybackProcedures.PLAYING, PlaybackProcedures.phase(start, start, dur));
        assertEquals(754_000, PlaybackProcedures.playbackPosition(start + 754_000, start, dur));
        assertEquals(PlaybackProcedures.ENDED, PlaybackProcedures.phase(start + dur, start, dur));
        assertEquals(dur, PlaybackProcedures.playbackPosition(start + dur + 1, start, dur));
    }

    @Test
    public void seekOnlyWhenDriftExceedsThreshold() {
        assertEquals(754_000, PlaybackProcedures.seekTarget(751_500, 754_000, 2000));
        assertEquals(PlaybackProcedures.NO_SEEK, PlaybackProcedures.seekTarget(753_200, 754_000, 2000));
        assertEquals(PlaybackProcedures.NO_SEEK, PlaybackProcedures.seekTarget(752_000, 754_000, 2000));
    }
}
