package ca.dev9.tranquil;

import ca.dev9.tranquil.blocks.Block;
import ca.dev9.tranquil.utils.Int3;

import java.util.ArrayList;

/**
 * Created by Zaneris on 02/07/2015.
 */
public class World {
	public static final byte WORLD_HEIGHT = 2; // Height of world in chunks
	public static final byte CHUNK_SIZE = 16;
	public static final byte FRAMES_PER_CYCLE = 10;
	public static final float TERRAIN_INTENSITY = 0.005f;
	public static final float TERRAIN_INTENSITY2 = 0.015f;
	public static final byte WATER_HEIGHT = 15;
	public static final short DRAW_DISTANCE = 100;
	public static final boolean TEXTURES_ON = false;
	public static final ChunkMap<Integer,Chunk> chunkMap = new ChunkMap<Integer,Chunk>();
	public static final ArrayList<Chunk> buildQueue = new ArrayList<Chunk>();
	public static final ArrayList<Chunk> faceQueue = new ArrayList<Chunk>();
	public static final ArrayList<Chunk> meshQueue = new ArrayList<Chunk>();
	public static double seed;

	public static void createNewWorld() {
		seed = Math.random()*10000d;
	}

	private static Chunk chunk;
	private static final Int3 i = new Int3();
	private static Int3 p;
	private static int j;
	public static void buildChunks() {
		if(!buildQueue.isEmpty()) {
			chunk = buildQueue.get(0);
			p = chunk.position;
			for (i.x = 0; i.x < Chunk.CHUNK_SIZE; i.x++)
				for (i.z = 0; i.z < Chunk.CHUNK_SIZE; i.z++) {
					j = terrainHeight(i.x + p.x, i.z + p.z);
					for (i.y = 0; i.y < Chunk.CHUNK_SIZE; i.y++) {
						if (i.y + p.y < j) {
						// || (i.y + p.y == j && i.y + p.y < WATER_HEIGHT))
							chunk.createBlock(Block.DIRT, i);
						} else if (i.y + p.y == j) {
							chunk.createBlock(Block.GRASS, i);
						} else {
							if(i.y + p.y <= WATER_HEIGHT)
								chunk.createBlock(Block.WATER, i);
							else
								chunk.createBlock(Block.AIR, i);
						}
					}
				}
			faceQueue.add(chunk);
			buildQueue.remove(0);
		}
	}

	private static double noise;
	public static short terrainHeight(int x, int z) {
		noise = (1d + SimplexNoise.noise(
				seed+(x*TERRAIN_INTENSITY),
				seed+(z*TERRAIN_INTENSITY)))
				*WORLD_HEIGHT*CHUNK_SIZE/4;
		noise += (1d + SimplexNoise.noise(
				seed+(x*TERRAIN_INTENSITY2),
				seed+(z*TERRAIN_INTENSITY2)))
				*WORLD_HEIGHT*CHUNK_SIZE/4;
		if(noise <1d)
			return 1;
		if(noise >=WORLD_HEIGHT*CHUNK_SIZE)
			return WORLD_HEIGHT*CHUNK_SIZE-1;
		return (short)Math.floor(noise);
	}

	public static void createMeshes() {
		if(!meshQueue.isEmpty()) {
			chunk = meshQueue.get(0);
			ChunkMeshGenerator.createMesh(chunk);
			chunk.wait = false;
			meshQueue.remove(0);
		}
	}

	private static Block block1;
	private static Block block2;
	private static boolean solid1;
	private static boolean solid2;
	public static void updateFaces() {
		if(!faceQueue.isEmpty()) {
			chunk = faceQueue.get(0);
			p = chunk.position;
			for (i.x = 0 + p.x; i.x < p.x + CHUNK_SIZE; i.x++)
				for (i.z = 0 + p.z; i.z < p.z + CHUNK_SIZE; i.z++)
					for (i.y = 0 + p.y; i.y < p.y + CHUNK_SIZE; i.y++) {
						block1 = getBlock(i.x, i.y, i.z);
						if (block1 != null) {
							block2 = getBlock(i.x + 1, i.y, i.z);
							setFlags(Block.FACE_EAST, Block.FACE_WEST);
							block2 = getBlock(i.x - 1, i.y, i.z);
							setFlags(Block.FACE_WEST, Block.FACE_EAST);
							block2 = getBlock(i.x, i.y, i.z + 1);
							setFlags(Block.FACE_SOUTH, Block.FACE_NORTH);
							block2 = getBlock(i.x, i.y, i.z - 1);
							setFlags(Block.FACE_NORTH, Block.FACE_SOUTH);
							block2 = getBlock(i.x, i.y - 1, i.z);
							setFlags(Block.FACE_TOP, Block.FACE_BOTTOM);
							block2 = getBlock(i.x, i.y + 1, i.z);
							setFlags(Block.FACE_BOTTOM, Block.FACE_TOP);
						}
					}
			faceQueue.remove(0);
		}
	}

	private static void setFlags(byte face1, byte face2) {
		if(block2!=null) {
			if(block1.blockType!=Block.WATER || block2.blockType!=Block.WATER) {
				solid1 = block1.hasFlag(Block.SOLID) ||
						(block1.blockType == Block.WATER &&
								block2.blockType == Block.AIR);
				solid2 = block2.hasFlag(Block.SOLID) ||
						(block1.blockType == Block.AIR &&
								block2.blockType == Block.WATER);
				if (solid2) block2.setFlag(solid1, face1);
				if (solid1) block1.setFlag(solid2, face2);
			}
		}
	}


	private static final Int3 temp = new Int3();
	public static Block getBlock(int x, int y, int z) {
		temp.set(x, y, z);
		return getBlock(temp);
	}
	private static Chunk tB;
	private static final Int3 inner = new Int3();
	public static Block getBlock(Int3 int3) {
		inner.copyFrom(int3);
		inner.mod(CHUNK_SIZE);
		int3.div(CHUNK_SIZE);
		tB = chunkMap.get(int3);
		if(tB==null)
			return null;
		return tB.getBlock(inner);
	}
}