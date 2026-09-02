export type SoundOption = {
  id: string;
  name: string;
  seconds: number;
  module: number;
};

export const SOUNDS: SoundOption[] = [
  { id: 'drop1', name: 'Water drop 1', seconds: 1, module: require('../../assets/sounds/water-drop-1.wav') },
  { id: 'drop2', name: 'Water drop 2', seconds: 3, module: require('../../assets/sounds/water-drop-2.wav') },
  { id: 'flow1', name: 'Water flowing 1', seconds: 5, module: require('../../assets/sounds/water-flowing-1.wav') },
  { id: 'flow2', name: 'Water flowing 2', seconds: 7, module: require('../../assets/sounds/water-flowing-2.wav') },
  { id: 'flow3', name: 'Water flowing 3', seconds: 9, module: require('../../assets/sounds/water-flowing-3.wav') },
];

export const soundById = (id: string) => SOUNDS.find((s) => s.id === id) ?? SOUNDS[0];
